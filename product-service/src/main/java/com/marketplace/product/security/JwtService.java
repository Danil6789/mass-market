package com.marketplace.product.security;

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

/**
 * Parses JWT tokens issued by user-service. Signature verification is NOT
 * performed here — Phase 8 will add api-gateway as the single verification
 * point. For now the filter only extracts the claims so the controllers can
 * know the current user.
 *
 * <p>Uses jjwt 0.12 API ({@link io.jsonwebtoken.Jwts#parser()}).</p>
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
     * Parse the token and return the claims. In Phase 4 we trust that the
     * upstream gateway has already verified the signature; we still call
     * {@code verifyWith} so an obviously wrong issuer / malformed token is
     * rejected locally.
     */
    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}