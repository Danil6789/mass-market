package com.marketplace.notification.email;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;

import java.time.Instant;

/**
 * Audit record for every attempted email send. The admin REST endpoint
 * surfaces this entity so admins can inspect what was sent and to whom.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Entity
@Table(
        name = "email_log",
        indexes = {
                @Index(name = "idx_email_log_recipient", columnList = "recipient"),
                @Index(name = "idx_email_log_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class EmailLog {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** Convenience factory for a successful send. */
    public static EmailLog success(String recipient, String subject, String body) {
        return EmailLog.builder()
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(EmailStatus.SENT)
                .build();
    }

    /** Convenience factory for a failed send (subject/body can be placeholder strings). */
    public static EmailLog failure(String recipient, String subject, String body, String errorMessage) {
        return EmailLog.builder()
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(EmailStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }
}
