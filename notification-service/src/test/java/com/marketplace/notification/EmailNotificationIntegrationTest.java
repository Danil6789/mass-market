package com.marketplace.notification;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.UserRegisteredEvent;
import com.marketplace.notification.email.EmailService;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * End-to-end integration test for the notification-service Kafka consumers.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context (which
 * wires the {@code @KafkaListener} consumer for
 * {@link KafkaTopics#USER_REGISTERED}), mocks {@link EmailService} to avoid
 * hitting SMTP, publishes a real {@link UserRegisteredEvent} to Kafka, and
 * asserts that the listener invokes {@code EmailService.send(...)} within
 * a reasonable timeout.</p>
 *
 * <p>Disabled in the default build because Postgres/Kafka are not available
 * locally. Remove {@code @Disabled} to run this in CI.</p>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class EmailNotificationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @MockBean EmailService emailService;

    @Test
    void userRegisteredEvent_triggersWelcomeEmail() {
        // Build a KafkaTemplate that points at the Testcontainers broker.
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        );
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    42L,
                    "alice@example.com",
                    "Alice",
                    Instant.parse("2024-01-01T00:00:00Z"));

            template.send(new ProducerRecord<>(KafkaTopics.USER_REGISTERED,
                    String.valueOf(event.userId()), event)).join();

            // Assert that the @KafkaListener consumed the event and called EmailService.
            ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> model =
                    ArgumentCaptor.forClass(Map.class);

            verify(emailService, timeout(java.time.Duration.ofSeconds(30).toMillis()))
                    .send(eventType.capture(), recipient.capture(), model.capture());

            assertThat(eventType.getValue()).isEqualTo("WELCOME");
            assertThat(recipient.getValue()).isEqualTo("alice@example.com");
            assertThat(model.getValue()).containsEntry("name", "Alice");
            assertThat(model.getValue()).containsEntry("email", "alice@example.com");
        } finally {
            template.destroy();
        }
    }
}