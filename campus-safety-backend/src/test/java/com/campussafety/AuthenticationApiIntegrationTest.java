package com.campussafety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationApiIntegrationTest {

    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper mapper;

    @Test
    void registrationLoginAndProtectedProfileNeverExposePasswordHash() throws Exception {
        String email = "student-" + UUID.randomUUID() + "@example.edu";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> denied = http.getForEntity("/api/v1/auth/me", String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> registration = http.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(Map.of("firstName", "Sam", "lastName", "Student", "email", email,
                        "password", "CampusTest#123"), headers), String.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = mapper.readTree(registration.getBody()).get("token").asText();

        headers.setBearerAuth(token);
        ResponseEntity<String> profile = http.exchange("/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        JsonNode user = mapper.readTree(profile.getBody());
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(user.get("role").asText()).isEqualTo("STUDENT");
        assertThat(user.has("password")).isFalse();

        ResponseEntity<String> updatedProfile = http.exchange("/api/v1/auth/me", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("phone", "555-0100", "department", "Student Services"), headers),
                String.class);
        assertThat(updatedProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(updatedProfile.getBody()).get("department").asText())
                .isEqualTo("Student Services");

        ResponseEntity<String> login = http.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", "CampusTest#123"), headers), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        for (String role : java.util.List.of("ADMIN", "SECURITY_OFFICER")) {
            ResponseEntity<String> escalation = http.postForEntity("/api/v1/auth/register",
                    new HttpEntity<>(Map.of("firstName", "Bad", "lastName", "Actor",
                            "email", "escalation-" + UUID.randomUUID() + "@example.edu",
                            "password", "CampusTest#123", "role", role), headers), String.class);
            assertThat(escalation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        ResponseEntity<String> invalidLogin = http.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", "incorrect-password"), headers), String.class);
        assertThat(invalidLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthIsPublicWithoutComponentDetailsAndCorsAllowsOnlyConfiguredFrontend() {
        ResponseEntity<String> health = http.getForEntity("/actuator/health", String.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"").doesNotContain("\"components\"");

        HttpHeaders allowedOriginHeaders = new HttpHeaders();
        allowedOriginHeaders.setOrigin("http://127.0.0.1:5173");
        allowedOriginHeaders.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> allowedOrigin = http.exchange("/api/v1/incidents", HttpMethod.OPTIONS,
                new HttpEntity<>(allowedOriginHeaders), String.class);
        assertThat(allowedOrigin.getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://127.0.0.1:5173");

        HttpHeaders untrustedOriginHeaders = new HttpHeaders();
        untrustedOriginHeaders.setOrigin("http://127.0.0.1:5174");
        untrustedOriginHeaders.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> untrustedOrigin = http.exchange("/api/v1/incidents", HttpMethod.OPTIONS,
                new HttpEntity<>(untrustedOriginHeaders), String.class);
        assertThat(untrustedOrigin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(untrustedOrigin.getHeaders().getAccessControlAllowOrigin()).isNull();
    }
}
