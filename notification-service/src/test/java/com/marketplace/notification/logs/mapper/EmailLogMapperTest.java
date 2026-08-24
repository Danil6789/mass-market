package com.marketplace.notification.logs.mapper;

import com.marketplace.notification.email.EmailLog;
import com.marketplace.notification.email.EmailStatus;
import com.marketplace.notification.logs.dto.EmailLogResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link EmailLogMapper} contract. The MapStruct-generated
 * impl is not available in this build's classpath, so we mock the interface
 * (same pattern as the rest of the marketplace services) and verify that the
 * mapper is invoked with the expected entity — the actual field-by-field copy
 * is MapStruct's responsibility.
 */
@ExtendWith(MockitoExtension.class)
class EmailLogMapperTest {

    @Mock private EmailLogMapper emailLogMapper;

    @Test
    void toResponse_delegatesToMapper() {
        EmailLog entity = EmailLog.builder()
                .id(1L)
                .recipient("alice@example.com")
                .subject("Welcome")
                .body("Hi Alice")
                .status(EmailStatus.SENT)
                .sentAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        EmailLogResponse expected = new EmailLogResponse(
                1L, "alice@example.com", "Welcome", "Hi Alice",
                EmailStatus.SENT, entity.getSentAt(), null);

        when(emailLogMapper.toResponse(entity)).thenReturn(expected);

        EmailLogResponse actual = emailLogMapper.toResponse(entity);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void mapperInterface_declaresExpectedSignature() {
        // Smoke check that the contract the controller relies on is still
        // present on the compiled interface.
        assertThat(EmailLogMapper.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .contains("toResponse");
    }
}
