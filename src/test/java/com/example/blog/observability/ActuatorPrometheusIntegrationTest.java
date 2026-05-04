package com.example.blog.observability;

import com.example.blog.global.security.jwt.JWTUtil;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import com.example.blog.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.task.scheduling.enabled=false",
                "management.server.port=0"
        }
)
class ActuatorPrometheusIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("blog_observability_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private Environment environment;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    void actuatorHealthEndpoint_requiresAuthenticationOnManagementPort() throws IOException, InterruptedException {
        Integer managementPort = environment.getProperty("local.management.port", Integer.class);
        assertThat(managementPort).isNotNull();

        HttpResponse<String> response = sendGet("http://localhost:" + managementPort + "/actuator/health");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void actuatorPrometheusEndpoint_exposesStandardAndOutboxMetricsWithAuthentication() throws IOException, InterruptedException {
        Integer managementPort = environment.getProperty("local.management.port", Integer.class);
        assertThat(managementPort).isNotNull();
        String bearerToken = "Bearer " + createAccessToken();

        HttpResponse<String> response = sendGet(
                "http://localhost:" + managementPort + "/actuator/prometheus",
                bearerToken
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).isPresent();
        assertThat(response.body())
                .contains("jvm_memory_used_bytes")
                .contains("hikaricp_connections")
                .contains("blog_search_outbox_pending_count")
                .contains("blog_search_outbox_failed_count");
    }

    private HttpResponse<String> sendGet(String url) throws IOException, InterruptedException {
        return sendGet(url, null);
    }

    private HttpResponse<String> sendGet(String url, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .GET();
        if (bearerToken != null) {
            builder.header("Authorization", bearerToken);
        }

        HttpRequest request = builder.build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String createAccessToken() {
        User user = new User();
        user.setEmail("observability-user@test.com");
        user.setNickname("observability-user");
        user.setUsername("observability-user");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.createJwt(user.getEmail(), "ROLE_" + user.getRole().name(), 1000L * 60 * 60);
    }
}
