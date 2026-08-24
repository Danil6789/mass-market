package com.marketplace.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SMTP sender properties. Bound to {@code app.mail.*} in
 * {@code application.yml}. The {@code from} address is required so every
 * outgoing email has a consistent envelope sender.
 */
@ConfigurationProperties(prefix = "app.mail")
@Validated
public class MailProperties {

    @NotBlank
    private String from;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
