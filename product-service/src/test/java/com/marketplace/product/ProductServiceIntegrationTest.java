package com.marketplace.product;

import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the product-service.
 *
 * <p>Spins up real PostgreSQL + Kafka via Testcontainers
 * ({@link ServiceConnection}), boots the full Spring Boot context against a
 * random port, mints a self-signed JWT for an admin, and exercises the
 * public category/product REST endpoints.</p>
 *
 * <p>Disabled in the default build because Postgres/Kafka are not available
 * locally. Remove {@code @Disabled} to run this in CI.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class ProductServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    int port;

    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired JwtProperties jwtProperties;

    TestRestTemplate http = new TestRestTemplate();

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void categoryCreate_thenProductCreate_thenList() {
        String adminToken = mintToken(1L, "admin@example.com", "ADMIN");
        String sellerToken = mintToken(42L, "alice@example.com", "USER");

        // 1. List categories (initially empty, public endpoint)
        ResponseEntity<Map[]> listCategories = http.getForEntity(url("/api/categories"), Map[].class);
        assertThat(listCategories.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listCategories.getBody()).isEmpty();

        // 2. Create a category as admin
        Map<String, Object> createCategory = Map.of("name", "Watches");
        ResponseEntity<Map> createCategoryResponse = http.exchange(
                url("/api/categories"),
                HttpMethod.POST,
                new HttpEntity<>(createCategory, jsonHeaders(adminToken)),
                Map.class);

        assertThat(createCategoryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createCategoryResponse.getBody()).isNotNull();
        Number categoryIdNumber = (Number) createCategoryResponse.getBody().get("id");
        assertThat(categoryIdNumber).isNotNull();
        Long categoryId = categoryIdNumber.longValue();

        // 3. Create a product as a regular seller (uses JWT principal as sellerId)
        Map<String, Object> createProduct = Map.of(
                "categoryId", categoryId,
                "title", "Vintage Watch",
                "description", "1962 Omega Seamaster",
                "price", 199.99
        );
        ResponseEntity<Map> createProductResponse = http.exchange(
                url("/api/products"),
                HttpMethod.POST,
                new HttpEntity<>(createProduct, jsonHeaders(sellerToken)),
                Map.class);

        assertThat(createProductResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createProductResponse.getBody()).isNotNull();
        assertThat(createProductResponse.getBody().get("title")).isEqualTo("Vintage Watch");
        Number productIdNumber = (Number) createProductResponse.getBody().get("id");
        assertThat(productIdNumber).isNotNull();
        Long productId = productIdNumber.longValue();

        // 4. List products (public endpoint)
        ResponseEntity<Map> listProducts = http.getForEntity(url("/api/products?page=0&size=20"), Map.class);
        assertThat(listProducts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listProducts.getBody()).isNotNull();
        assertThat(listProducts.getBody().get("content")).isNotNull();

        // 5. Get product by id (public endpoint)
        ResponseEntity<Map> getProduct = http.getForEntity(url("/api/products/" + productId), Map.class);
        assertThat(getProduct.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getProduct.getBody()).isNotNull();
        assertThat(getProduct.getBody().get("title")).isEqualTo("Vintage Watch");
    }

    @Test
    void getProductById_returnsNotFound_whenMissing() {
        ResponseEntity<Map> response = http.getForEntity(url("/api/products/999999"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createCategory_throwsForbidden_whenNotAdmin() {
        String userToken = mintToken(42L, "alice@example.com", "USER");
        Map<String, Object> createCategory = Map.of("name", "Watches");

        ResponseEntity<Map> response = http.exchange(
                url("/api/categories"),
                HttpMethod.POST,
                new HttpEntity<>(createCategory, jsonHeaders(userToken)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Mint a self-signed access JWT with the same shape as
     * {@code user-service}'s {@code JwtService#generateAccessToken}. Required
     * because product-service trusts signature only when paired with the same
     * shared {@code app.jwt.secret}.
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
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return headers;
    }
}