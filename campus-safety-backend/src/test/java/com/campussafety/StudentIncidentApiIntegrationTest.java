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
class StudentIncidentApiIntegrationTest {

    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper mapper;

    @Test
    void studentReportPersistsAndIncidentListsRespectReporterOwnership() throws Exception {
        String reporterToken = register("reporter");
        String otherToken = register("other");
        HttpHeaders headers = bearer(reporterToken);
        Map<String, Object> request = Map.of(
                "category", "MEDICAL_EMERGENCY",
                "description", "A person needs first aid near the library.",
                "locationName", "Main Library, north entrance",
                "severity", "HIGH");
        ResponseEntity<String> created = http.postForEntity("/api/v1/incidents",
                new HttpEntity<>(request, headers), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode createdIncident = mapper.readTree(created.getBody());
        long incidentId = createdIncident.get("id").asLong();
        assertThat(createdIncident.get("reporter").get("password")).isNull();

        ResponseEntity<String> mine = http.exchange("/api/v1/incidents/my", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(mine.getBody()).get("content").toString()).contains("\"id\":" + incidentId);

        ResponseEntity<String> otherList = http.exchange("/api/v1/incidents", HttpMethod.GET,
                new HttpEntity<>(bearer(otherToken)), String.class);
        assertThat(otherList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(otherList.getBody()).get("content")).isEmpty();

        ResponseEntity<String> filter = http.exchange("/api/v1/incidents?category=MEDICAL_EMERGENCY&severity=HIGH",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(filter.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(filter.getBody()).get("content").get(0).get("id").asLong()).isEqualTo(incidentId);

        ResponseEntity<String> invalidPage = http.exchange("/api/v1/incidents?page=-1", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(invalidPage.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String register(String name) throws Exception {
        String email = name + "-" + UUID.randomUUID() + "@example.edu";
        ResponseEntity<String> response = http.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(Map.of("firstName", name, "lastName", "Test", "email", email,
                        "password", "CampusTest#123"), jsonHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("token").asText();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
