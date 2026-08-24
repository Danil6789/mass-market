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
import com.marketplace.user.kafka.UserEventProducer;
import com.marketplace.user.mapper.UserMapper;
import com.marketplace.user.profile.dto.UserProfileResponse;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}. All collaborators are mocked; no
 * Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserEventProducer userEventProducer;

    @InjectMocks private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(42L)
                .email("alice@example.com")
                .password("hashed-password")
                .name("Alice")
                .role(User.Role.USER)
                .active(true)
                .blocked(false)
                .build();
    }

    // ------------------------------------------------------------------ register

    @Test
    void register_persistsUser_publishesEvent_returnsTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("plain-password");
        req.setName("Alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(req)).thenReturn(User.builder().email("alice@example.com").build());
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(42L, "alice@example.com", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(42L)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toAuthResponse(sampleUser, "access-token", "refresh-token", 900L))
                .thenReturn(new AuthResponse("access-token", "refresh-token", 900L, "Bearer",
                        new UserProfileResponse(42L, "alice@example.com", "Alice", null, "USER", true, false, null, null)));

        AuthResponse response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("alice@example.com");

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(userEventProducer).publishUserRegistered(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().email()).isEqualTo("alice@example.com");
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("plain-password");
        req.setName("Alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userEventProducer, never()).publishUserRegistered(any());
    }

    @Test
    void register_throwsEmailAlreadyExists_onRaceConditionUniqueViolation() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPassword("plain-password");
        req.setName("Alice");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(req)).thenReturn(User.builder().email("alice@example.com").build());
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique email"));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // ------------------------------------------------------------------ login

    @Test
    void login_returnsTokens_whenCredentialsValid() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("plain-password");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(42L, "alice@example.com", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(42L)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toAuthResponse(sampleUser, "access-token", "refresh-token", 900L))
                .thenReturn(new AuthResponse("access-token", "refresh-token", 900L, "Bearer",
                        new UserProfileResponse(42L, "alice@example.com", "Alice", null, "USER", true, false, null, null)));

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_throwsBadCredentials_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("plain-password");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentials_whenPasswordDoesNotMatch() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("wrong-password");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsUserBlocked_whenAccountInactive() {
        sampleUser.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("plain-password");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserBlockedException.class);
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refresh_returnsNewTokens_whenRefreshTokenValid() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-refresh-token");

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("42");
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(jwtService.parseClaims("valid-refresh-token")).thenReturn(claims);
        when(userRepository.findById(42L)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(42L, "alice@example.com", "USER")).thenReturn("new-access");
        when(jwtService.generateRefreshToken(42L)).thenReturn("new-refresh");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toAuthResponse(sampleUser, "new-access", "new-refresh", 900L))
                .thenReturn(new AuthResponse("new-access", "new-refresh", 900L, "Bearer",
                        new UserProfileResponse(42L, "alice@example.com", "Alice", null, "USER", true, false, null, null)));

        AuthResponse response = authService.refresh(req);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenInvalid() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("garbage");

        when(jwtService.parseClaims("garbage")).thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_throwsBadCredentials_whenTokenTypeIsAccess() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("access-token-pretending-to-be-refresh");

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("access");
        when(jwtService.parseClaims("access-token-pretending-to-be-refresh")).thenReturn(claims);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findById(anyLong());
    }
}