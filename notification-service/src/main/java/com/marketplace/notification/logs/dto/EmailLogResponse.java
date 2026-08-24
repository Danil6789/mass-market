package com.marketplace.notification.logs.dto;

import com.marketplace.notification.email.EmailStatus;

import java.time.Instant;

/**
 * Response payload for the admin email-log endpoint. Java record (immutable)
 * per marketplace convention for response DTOs.
 */
public record EmailLogResponse(
        Long id,
        String recipient,
        String subject,
        String body,
        EmailStatus status,
        Instant sentAt,
        String errorMessage
) {
}
