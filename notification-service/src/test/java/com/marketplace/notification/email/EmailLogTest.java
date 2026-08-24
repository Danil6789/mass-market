package com.marketplace.notification.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmailLog} builder factories.
 */
class EmailLogTest {

    @Test
    void success_setsStatusAndFields() {
        EmailLog log = EmailLog.success("alice@example.com", "Welcome", "Hi Alice!");

        assertThat(log.getRecipient()).isEqualTo("alice@example.com");
        assertThat(log.getSubject()).isEqualTo("Welcome");
        assertThat(log.getBody()).isEqualTo("Hi Alice!");
        assertThat(log.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void failure_setsStatusAndErrorMessage() {
        EmailLog log = EmailLog.failure("bob@example.com", "Welcome", "Hi Bob!", "Connection refused");

        assertThat(log.getRecipient()).isEqualTo("bob@example.com");
        assertThat(log.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(log.getErrorMessage()).isEqualTo("Connection refused");
    }
}
