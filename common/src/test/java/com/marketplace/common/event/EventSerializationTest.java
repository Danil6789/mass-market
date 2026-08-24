package com.marketplace.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity tests that every Kafka event record round-trips through Jackson and that
 * null fields are omitted from the serialized JSON (per the {@code @JsonInclude}
 * annotation on each event).
 *
 * <p>If these tests pass, the cross-service JSON contract is stable: producers
 *     in any service will produce JSON that consumers in any other service can
 *     parse.</p>
 */
class EventSerializationTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private <T> T roundTrip(T event, Class<T> type) throws Exception {
        String json = objectMapper.writeValueAsString(event);
        return objectMapper.readValue(json, type);
    }

    @Test
    @DisplayName("UserRegisteredEvent round-trip preserves fields and omits nulls")
    void userRegisteredRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        UserRegisteredEvent original = new UserRegisteredEvent(42L, "alice@example.com", "Alice", ts);

        String json = objectMapper.writeValueAsString(original);

        assertTrue(json.contains("\"userId\":42"), "userId should be present");
        assertTrue(json.contains("\"email\":\"alice@example.com\""), "email should be present");
        assertTrue(json.contains("\"name\":\"Alice\""), "name should be present");
        assertTrue(json.contains("\"createdAt\":"), "createdAt should be present");

        UserRegisteredEvent parsed = roundTrip(original, UserRegisteredEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("UserBlockedEvent round-trip")
    void userBlockedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        UserBlockedEvent original = new UserBlockedEvent(7L, 1L, "spam", ts);

        UserBlockedEvent parsed = roundTrip(original, UserBlockedEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("UserUnblockedEvent round-trip")
    void userUnblockedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        UserUnblockedEvent original = new UserUnblockedEvent(7L, 1L, ts);

        UserUnblockedEvent parsed = roundTrip(original, UserUnblockedEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("ProductCreatedEvent serializes BigDecimal price as a JSON number string")
    void productCreatedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        ProductCreatedEvent original = new ProductCreatedEvent(
                100L, 5L, "Vintage Watch", new BigDecimal("199.99"), 3L, ts);

        String json = objectMapper.writeValueAsString(original);
        // BigDecimal is serialized as a JSON number (Jackson default); ensure value survives.
        assertTrue(json.contains("199.99"), "price should serialize as a JSON number: " + json);

        ProductCreatedEvent parsed = roundTrip(original, ProductCreatedEvent.class);
        assertEquals(0, original.price().compareTo(parsed.price()),
                "price must round-trip with exact precision");
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("ProductDeletedEvent round-trip")
    void productDeletedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        ProductDeletedEvent original = new ProductDeletedEvent(100L, 5L, 1L, "duplicate listing", ts);

        ProductDeletedEvent parsed = roundTrip(original, ProductDeletedEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("ProductStatusChangedEvent round-trip")
    void productStatusChangedRoundTrip() throws Exception {
        ProductStatusChangedEvent original = new ProductStatusChangedEvent(
                100L, "ACTIVE", "RESERVED", 555L);

        ProductStatusChangedEvent parsed = roundTrip(original, ProductStatusChangedEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("OrderCreatedEvent round-trip with BigDecimal amount")
    void orderCreatedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        OrderCreatedEvent original = new OrderCreatedEvent(
                555L, 7L, 5L, 100L, new BigDecimal("199.99"), ts);

        OrderCreatedEvent parsed = roundTrip(original, OrderCreatedEvent.class);
        assertEquals(original, parsed);
        assertEquals(0, original.amount().compareTo(parsed.amount()));
    }

    @Test
    @DisplayName("OrderPaidEvent round-trip with String paymentId")
    void orderPaidRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        OrderPaidEvent original = new OrderPaidEvent(
                555L, 7L, 5L, 100L, new BigDecimal("199.99"), ts, "pay-abc-def");

        String json = objectMapper.writeValueAsString(original);
        assertTrue(json.contains("\"paymentId\":\"pay-abc-def\""), "paymentId should be a string");

        OrderPaidEvent parsed = roundTrip(original, OrderPaidEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("OrderCancelledEvent round-trip")
    void orderCancelledRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        OrderCancelledEvent original = new OrderCancelledEvent(
                555L, 7L, 5L, 100L, 1L, "buyer changed mind", ts);

        OrderCancelledEvent parsed = roundTrip(original, OrderCancelledEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("OrderFailedEvent round-trip")
    void orderFailedRoundTrip() throws Exception {
        Instant ts = Instant.parse("2026-08-24T10:15:30Z");
        OrderFailedEvent original = new OrderFailedEvent(555L, 7L, "card declined", ts);

        OrderFailedEvent parsed = roundTrip(original, OrderFailedEvent.class);
        assertEquals(original, parsed);
    }

    @Test
    @DisplayName("Null fields are omitted from JSON output (NON_NULL include policy)")
    void nullFieldsAreOmitted() throws Exception {
        // All fields null except orderId — only orderId should appear in JSON.
        OrderFailedEvent partial = new OrderFailedEvent(555L, null, null, null);
        String json = objectMapper.writeValueAsString(partial);

        assertTrue(json.contains("\"orderId\":555"), "orderId should be present");
        assertFalse(json.contains("buyerId"), "buyerId should be omitted when null");
        assertFalse(json.contains("reason"), "reason should be omitted when null");
        assertFalse(json.contains("timestamp"), "timestamp should be omitted when null");

        // And the record still round-trips correctly with nulls intact.
        OrderFailedEvent parsed = objectMapper.readValue(json, OrderFailedEvent.class);
        assertNotNull(parsed);
        assertEquals(555L, parsed.orderId());
        assertNull(parsed.buyerId());
        assertNull(parsed.reason());
        assertNull(parsed.timestamp());
    }
}