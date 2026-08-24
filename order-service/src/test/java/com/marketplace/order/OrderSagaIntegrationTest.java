package com.marketplace.order;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.client.ProductSnapshot;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test for the order-service saga.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context against a
 * random port, mocks the OpenFeign {@link ProductClient} so we don't need a
 * running product-service, mints a self-signed JWT for a buyer, and asserts
 * that {@code POST /api/orders} persists an order AND publishes an
 * {@link OrderCreatedEvent} to Kafka that we can read back with a fresh
 * consumer.</p>
 *
 * <p>Disabled in the default build because Postgres/Kafka are not available
 * locally. Remove {@code @Disabled} to run this in CI.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class OrderSagaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    int port;

    @MockBean ProductClient productClient;

    @Autowired OrderRepository orderRepository;
    @Autowired JwtProperties jwtProperties;

    TestRestTemplate http = new TestRestTemplate();

    @BeforeEach
    void cleanDb() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_persistsAndPublishesOrderCreatedEvent() {
        // Arrange: configure the mocked Feign client to return an ACTIVE product
        ProductSnapshot activeProduct = new ProductSnapshot(
                7L, 20L, new BigDecimal("250.00"), "ACTIVE");
        when(productClient.getProduct(anyLong())).thenReturn(activeProduct);

        String buyerToken = mintToken(10L, "buyer@example.com", "USER");
        Map<String, Object> requestBody = Map.of("productId", 7);

        // Act: POST /api/orders
        ResponseEntity<Map> response = http.exchange(
                url("/api/orders"),
                HttpMethod.POST,
                new HttpEntity<>(requestBody, jsonHeaders(buyerToken)),
                Map.class);

        // Assert: order persisted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        Number orderIdNumber = (Number) response.getBody().get("id");
        assertThat(orderIdNumber).isNotNull();
        Long orderId = orderIdNumber.longValue();

        assertThat(orderRepository.findById(orderId))
                .isPresent()
                .get()
                .satisfies(o -> {
                    assertThat(o.getStatus().name()).isEqualTo("PENDING");
                    assertThat(o.getBuyerId()).isEqualTo(10L);
                    assertThat(o.getSellerId()).isEqualTo(20L);
                    assertThat(o.getProductId()).isEqualTo(7L);
                    assertThat(o.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
                });

        // Assert: OrderCreatedEvent was published to Kafka — consume it back
        consumeOrderCreatedEvent(orderId);
    }

    @Test
    void createOrder_rejects_whenProductInactive() {
        ProductSnapshot reservedProduct = new ProductSnapshot(
                7L, 20L, new BigDecimal("250.00"), "RESERVED");
        when(productClient.getProduct(anyLong())).thenReturn(reservedProduct);

        String buyerToken = mintToken(10L, "buyer@example.com", "USER");
        Map<String, Object> requestBody = Map.of("productId", 7);

        ResponseEntity<Map> response = http.exchange(
                url("/api/orders"),
                HttpMethod.POST,
                new HttpEntity<>(requestBody, jsonHeaders(buyerToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(orderRepository.findAll()).isEmpty();
    }

    /**
     * Subscribe to the {@code order.created} topic and assert that an event
     * for the supplied order id is received within 30s.
     */
    private void consumeOrderCreatedEvent(Long expectedOrderId) {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName(),
                JsonDeserializer.TRUSTED_PACKAGES, "com.marketplace.common.event",
                JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName(),
                JsonDeserializer.USE_TYPE_INFO_HEADERS, false
        );

        try (KafkaConsumer<String, OrderCreatedEvent> consumer =
                     new KafkaConsumer<>(consumerProps, new StringDeserializer(),
                             new JsonDeserializer<>(OrderCreatedEvent.class, false))) {
            consumer.subscribe(Collections.singleton(KafkaTopics.ORDER_CREATED));

            long deadline = System.currentTimeMillis() + 30_000L;
            boolean found = false;
            while (System.currentTimeMillis() < deadline && !found) {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, OrderCreatedEvent> r : records) {
                    OrderCreatedEvent event = r.value();
                    if (event != null && event.orderId().equals(expectedOrderId)) {
                        assertThat(event.productId()).isEqualTo(7L);
                        assertThat(event.sellerId()).isEqualTo(20L);
                        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
                        found = true;
                        break;
                    }
                }
            }
            assertThat(found).as("OrderCreatedEvent for orderId=" + expectedOrderId).isTrue();
        }
    }

    private String mintToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static HttpHeaders jsonHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return headers;
    }
}