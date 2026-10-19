package com.campussafety;

import com.campussafety.config.AdminBootstrapRunner;
import com.campussafety.entity.Role;
import com.campussafety.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:campus_safety_bootstrap_test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "app.bootstrap-admin.enabled=true",
        "app.bootstrap-admin.email=bootstrap-admin@example.edu",
        "app.bootstrap-admin.password=CampusSafetyBootstrapPassword#2026"
})
class AdminBootstrapIntegrationTest {

    private static final String BOOTSTRAP_PASSWORD = "CampusSafetyBootstrapPassword#2026";

    @Autowired private AdminBootstrapRunner bootstrapRunner;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper mapper;

    @Test
    void createsHashedAdminOnceAndAllowsAdminLogin() throws Exception {
        var administrator = userRepository.findByEmailIgnoreCase("bootstrap-admin@example.edu").orElseThrow();
        assertThat(administrator.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(BOOTSTRAP_PASSWORD, administrator.getPassword())).isTrue();
        String storedHash = administrator.getPassword();

        ApplicationArguments noArguments = new DefaultApplicationArguments();
        bootstrapRunner.run(noArguments);
        assertThat(userRepository.findByEmailIgnoreCase("bootstrap-admin@example.edu").orElseThrow().getPassword())
                .isEqualTo(storedHash);

        ResponseEntity<String> login = http.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", "bootstrap-admin@example.edu", "password", BOOTSTRAP_PASSWORD),
                        jsonHeaders()), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode response = mapper.readTree(login.getBody());
        String token = response.get("token").asText();

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> dashboard = http.exchange("/api/v1/admin/dashboard", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody()).doesNotContain(BOOTSTRAP_PASSWORD, storedHash);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
