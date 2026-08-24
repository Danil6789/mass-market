package com.marketplace.notification.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT-related configuration properties. Bound to {@code app.jwt.*} in
 * {@code application.yml}.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters for HS256")
    private String secret;

    @NotBlank
    private String issuer = "marketplace";

    private long accessTokenTtlMinutes = 15;

    private long refreshTokenTtlDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public long getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60L;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlDays * 24L * 60L * 60L;
    }
}
