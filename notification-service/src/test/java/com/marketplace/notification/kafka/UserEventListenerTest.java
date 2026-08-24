package com.marketplace.notification.kafka;

import com.marketplace.common.event.UserRegisteredEvent;
import com.marketplace.notification.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link UserEventListener}.
 */
@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock private EmailService emailService;

    @InjectMocks private UserEventListener listener;

    @Test
    void onUserRegistered_invokesEmailServiceWithWelcome() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                7L, "alice@example.com", "Alice", Instant.parse("2024-01-01T00:00:00Z"));

        listener.onUserRegistered(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> recipientCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).send(eventTypeCaptor.capture(), recipientCaptor.capture(), modelCaptor.capture());

        assertThat(eventTypeCaptor.getValue()).isEqualTo("WELCOME");
        assertThat(recipientCaptor.getValue()).isEqualTo("alice@example.com");
        assertThat(modelCaptor.getValue()).containsEntry("name", "Alice");
        assertThat(modelCaptor.getValue()).containsEntry("email", "alice@example.com");
    }

    @Test
    void onUserRegistered_handlesNullName() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                7L, "bob@example.com", null, Instant.now());

        listener.onUserRegistered(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("bob@example.com"),
                modelCaptor.capture());
        assertThat(modelCaptor.getValue()).containsEntry("name", "");
    }
}
