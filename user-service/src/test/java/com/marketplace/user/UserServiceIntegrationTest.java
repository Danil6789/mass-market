package com.marketplace.user;

import com.marketplace.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the authentication flow.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context against a
 * random port, and exercises the public {@code /api/auth/register} and
 * {@code /api/auth/login} endpoints using {@link TestRestTemplate}.</p>
 *
 * <p>Disabled in the default build because Postgres/Kafka are not available
 * locally. Remove {@code @Disabled} (or pass {@code -DdockerAvailable=true}
 * with the {@code @EnabledIfDockerAvailable} form) to run this in CI.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class UserServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    int port;

    @Autowired UserRepository userRepository;

    TestRestTemplate http = new TestRestTemplate();

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    void register_thenLogin_returnsTokens() {
        String email = "alice@example.com";
        String password = "P@ssw0rd!";

        Map<String, Object> registerBody = Map.of(
                "email", email,
                "password", password,
                "name", "Alice"
        );

        HttpHeaders json = jsonHeaders();

        // Register
        ResponseEntity<Map> registerResponse = http.exchange(
                url("/api/auth/register"),
                HttpMethod.POST,
                new HttpEntity<>(registerBody, json),
                Map.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().get("accessToken")).asString().isNotBlank();
        assertThat(registerResponse.getBody().get("refreshToken")).asString().isNotBlank();

        // Login with the same credentials
        Map<String, Object> loginBody = Map.of(
                "email", email,
                "password", password
        );

        ResponseEntity<Map> loginResponse = http.exchange(
                url("/api/auth/login"),
                HttpMethod.POST,
                new HttpEntity<>(loginBody, json),
                Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().get("accessToken")).asString().isNotBlank();
        assertThat(loginResponse.getBody().get("refreshToken")).asString().isNotBlank();

        // Profile endpoint should accept the freshly issued token
        HttpHeaders auth = new HttpHeaders();
        auth.setBearerAuth((String) loginResponse.getBody().get("accessToken"));
        auth.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> meResponse = http.exchange(
                url("/api/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(auth),
                Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().get("email")).isEqualTo(email);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}