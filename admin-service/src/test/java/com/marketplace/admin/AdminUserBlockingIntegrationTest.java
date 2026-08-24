package com.marketplace.admin;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.client.UserClient;
import com.marketplace.admin.client.UserDto;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.kafka.AdminEventProducer;
import com.marketplace.admin.repository.AuditLogRepository;
import com.marketplace.admin.security.JwtProperties;
import com.marketplace.admin.users.service.AdminUserService;
import com.marketplace.common.event.UserBlockedEvent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test for the admin-service user-blocking flow.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context against a
 * random port, mocks the OpenFeign {@link UserClient} so we don't need a
 * running user-service, mints a self-signed JWT for an ADMIN user, exercises
 * {@code POST /api/admin/users/{id}/block}, and asserts that:</p>
 * <ul>
 *   <li>the controller returns {@code 202 Accepted};</li>
 *   <li>{@link AdminUserService} publishes a {@link UserBlockedEvent} via
 *       the mocked {@code AdminEventProducer};</li>
 *   <li>{@link AuditService} records an {@link AuditLog} row of action
 *       {@code BLOCK_USER}.</li>
 * </ul>
 *
 * <p>Disabled in the default build because Postgres/Kafka are not available
 * locally. Remove {@code @Disabled} to run this in CI.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class AdminUserBlockingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    int port;

    @MockBean UserClient userClient;
    @MockBean AuditService auditService;
    @MockBean AdminEventProducer adminEventProducer;

    @Autowired AuditLogRepository auditLogRepository;
    @Autowired JwtProperties jwtProperties;

    TestRestTemplate http = new TestRestTemplate();

    @BeforeEach
    void cleanDb() {
        auditLogRepository.deleteAll();
    }

    @Test
    void blockUser_returns202_publishesEvent_andRecordsAudit() {
        when(userClient.getUserById(anyLong()))
                .thenReturn(new UserDto(42L, "alice@example.com", false));

        String adminToken = mintToken(1L, "admin@example.com", "ADMIN");

        Map<String, Object> body = Map.of("reason", "spam");

        ResponseEntity<Void> response = http.exchange(
                url("/api/admin/users/42/block"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(adminToken)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // The service should have produced a UserBlockedEvent with the right shape.
        ArgumentCaptor<UserBlockedEvent> captor = ArgumentCaptor.forClass(UserBlockedEvent.class);
        verify(adminEventProducer).publishUserBlocked(captor.capture());
        UserBlockedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(42L);
        assertThat(event.blockedBy()).isEqualTo(1L);
        assertThat(event.reason()).isEqualTo("spam");
        assertThat(event.timestamp()).isNotNull();

        // Audit row was sent to AuditService with action=BLOCK_USER.
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).record(auditCaptor.capture());
        AuditLog row = auditCaptor.getValue();
        assertThat(row.getAdminId()).isEqualTo(1L);
        assertThat(row.getAction()).isEqualTo(AdminActions.BLOCK_USER);
        assertThat(row.getTargetType()).isEqualTo(TargetType.USER);
        assertThat(row.getTargetId()).isEqualTo(42L);
        assertThat(row.getDetails()).isEqualTo("reason=spam");
    }

    @Test
    void blockUser_returnsNotFound_whenUserMissing() {
        when(userClient.getUserById(anyLong())).thenReturn(null);

        String adminToken = mintToken(1L, "admin@example.com", "ADMIN");
        Map<String, Object> body = Map.of("reason", "spam");

        ResponseEntity<Map> response = http.exchange(
                url("/api/admin/users/999/block"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(adminToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void blockUser_returnsForbidden_whenCallerIsNotAdmin() {
        String userToken = mintToken(99L, "alice@example.com", "USER");
        Map<String, Object> body = Map.of("reason", "spam");

        ResponseEntity<Map> response = http.exchange(
                url("/api/admin/users/42/block"),
                HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(userToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Mint a self-signed access JWT with the same shape as
     * {@code user-service}'s {@code JwtService#generateAccessToken}.
     */
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