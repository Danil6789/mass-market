package com.marketplace.user.profile.api;

import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the profile endpoints.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context against a
 * random port and exercises {@code GET /api/users/me} and {@code PATCH ...}
 * using a freshly issued JWT.</p>
 *
 * <p>Disabled in the default build — Postgres/Kafka are not running here.
 * Remove {@code @Disabled} (or add a Maven/Gradle tag) to run this in CI.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class UserControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    int port;

    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;

    RestTemplate http = new RestTemplate();

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void getMe_returnsCurrentUser() {
        User user = userRepository.save(User.builder()
                .email("alice@example.com")
                .password("does-not-matter")
                .name("Alice")
                .role(User.Role.USER)
                .active(true)
                .blocked(false)
                .build());

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("email")).isEqualTo("alice@example.com");
        assertThat(((Number) response.getBody().get("id")).longValue()).isEqualTo(user.getId());
    }

    @Test
    void patchMe_updatesNameAndPhone() {
        User user = userRepository.save(User.builder()
                .email("alice@example.com")
                .password("does-not-matter")
                .name("Alice")
                .role(User.Role.USER)
                .active(true)
                .blocked(false)
                .build());

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of("name", "Alice Updated", "phone", "+7-900-000-00-00");

        ResponseEntity<Map> response = http.exchange(
                "http://localhost:" + port + "/api/users/me",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo("Alice Updated");
        assertThat(response.getBody().get("phone")).isEqualTo("+7-900-000-00-00");
    }
}