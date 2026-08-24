package com.marketplace.notification.email;

/**
 * Status of an outgoing email. Persisted as a string via {@code @Enumerated(STRING)}
 * — values must match the {@code CHECK} constraint on {@code email_log.status}.
 */
public enum EmailStatus {
    SENT,
    FAILED
}
