package com.marketplace.user.auth.service;

import com.marketplace.common.event.UserRegisteredEvent;
import com.marketplace.user.auth.dto.AuthResponse;
import com.marketplace.user.auth.dto.LoginRequest;
import com.marketplace.user.auth.dto.RefreshRequest;
import com.marketplace.user.auth.dto.RegisterRequest;
import com.marketplace.user.auth.exception.BadCredentialsException;
import com.marketplace.user.entity.User;
import com.marketplace.user.exception.EmailAlreadyExistsException;
import com.marketplace.user.exception.UserBlockedException;
import com.marketplace.user.exception.UserNotFoundException;
import com.marketplace.user.kafka.UserEventProducer;
import com.marketplace.user.mapper.UserMapper;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.marketplace.user.constant.ExceptionMessages.BAD_CREDENTIALS;
import static com.marketplace.user.constant.ExceptionMessages.EMAIL_ALREADY_EXISTS;
import static com.marketplace.user.constant.ExceptionMessages.USER_BLOCKED;
import static com.marketplace.user.constant.ExceptionMessages.USER_NOT_FOUND;

/**
 * Authentication / registration / refresh flows for the user-service.
 *
 * <p>Implements the public API contract documented under
 * {@code /api/auth/**} endpoints in {@code AuthApi}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserEventProducer userEventProducer;

    /**
     * Register a new user. Email uniqueness is enforced both at the
     * application layer (fast-fail) and at the DB level (race-safe).
     *
     * @throws EmailAlreadyExistsException if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setBlocked(false);

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Race-condition fallback: two concurrent registrations for the same email.
            log.warn("Duplicate email detected at insert time: {}", request.getEmail());
            throw new EmailAlreadyExistsException(EMAIL_ALREADY_EXISTS);
        }

        Instant occurredAt = saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now();
        userEventProducer.publishUserRegistered(new UserRegisteredEvent(
                saved.getId(), saved.getEmail(), saved.getName(), occurredAt
        ));

        String access = jwtService.generateAccessToken(saved.getId(), saved.getEmail(), saved.getRole().name());
        String refresh = jwtService.generateRefreshToken(saved.getId());

        log.info("User registered: id={}, email={}", saved.getId(), saved.getEmail());

        return userMapper.toAuthResponse(saved, access, refresh, jwtService.getAccessTokenTtlSeconds());
    }

    /**
     * Authenticate by email + password and issue fresh tokens.
     *
     * @throws BadCredentialsException when the email/password pair is wrong
     * @throws UserBlockedException    when the account has been blocked
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.debug("Login failed: user not found: {}", request.getEmail());
                    return new BadCredentialsException(BAD_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.debug("Login failed: bad password for {}", request.getEmail());
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        if (!user.isActive() || user.isBlocked()) {
            throw new UserBlockedException(USER_BLOCKED);
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getId());

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());

        return userMapper.toAuthResponse(user, access, refresh, jwtService.getAccessTokenTtlSeconds());
    }

    /**
     * Validate a refresh token and return a fresh access+refresh pair.
     *
     * @throws BadCredentialsException if the token is missing/invalid/expired
     *                                  or refers to a missing user
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.getRefreshToken());
        } catch (JwtException ex) {
            log.debug("Refresh failed: invalid token: {}", ex.getMessage());
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        Long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND));

        if (!user.isActive() || user.isBlocked()) {
            throw new UserBlockedException(USER_BLOCKED);
        }

        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getId());

        return userMapper.toAuthResponse(user, access, refresh, jwtService.getAccessTokenTtlSeconds());
    }
}