package com.marketplace.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and validates JWT tokens for the user-service. Uses jjwt 0.12 API
 * ({@link io.jsonwebtoken.Jwts#builder()}/{@link io.jsonwebtoken.Jwts#parser()}).
 *
 * <p>The {@code access} token carries {@code sub=email}, {@code userId},
 * {@code role} and {@code type=access}. The {@code refresh} token only
 * carries {@code sub=email} and {@code type=refresh} — it's looked up via
 * signature, not via DB.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256 (got " + bytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        log.info("JwtService initialised: issuer={}, accessTtl={}m, refreshTtl={}d",
                props.getIssuer(),
                props.getAccessTokenTtlMinutes(),
                props.getRefreshTokenTtlDays());
    }

    /**
     * Generate a short-lived access token containing the user identity and role.
     */
    public String generateAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Generate a long-lived refresh token. Only carries the subject (email).
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.getRefreshTokenTtlDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    /**
     * Validate signature, issuer and expiration; return the claims if valid.
     *
     * @throws JwtException if the token is malformed, has the wrong issuer,
     *                      has expired or fails signature verification.
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenTtlSeconds() {
        return props.getAccessTokenTtlSeconds();
    }
}