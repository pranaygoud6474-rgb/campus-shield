package com.campussafety;

import com.campussafety.entity.Role;
import com.campussafety.entity.User;
import com.campussafety.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResponderAdminApiIntegrationTest {

    @Autowired private TestRestTemplate http;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void roleMatrixEnforcesPermissionsAtTheApiForAnonymousStudentOfficerAndAdmin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User administrator = userRepository.save(new User("Campus", "Admin",
                "matrix-admin-" + suffix + "@example.edu", passwordEncoder.encode("CampusAdmin#123"), Role.ADMIN));
        User officer = userRepository.save(new User("Riley", "Officer",
                "matrix-officer-" + suffix + "@example.edu", passwordEncoder.encode("CampusOfficer#123"),
                Role.SECURITY_OFFICER));
        String studentEmail = "student-matrix-" + suffix + "@example.edu";
        String studentToken = registerStudent("matrix-" + suffix);
        String adminToken = login(administrator.getEmail(), "CampusAdmin#123");
        String officerToken = login(officer.getEmail(), "CampusOfficer#123");
        long studentId = userRepository.findByEmailIgnoreCase(studentEmail).orElseThrow().getId();

        assertStatus(HttpMethod.GET, "/api/v1/auth/me", null, null, HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.GET, "/api/v1/incidents", null, null, HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.GET, "/api/v1/officers", null, null, HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.GET, "/api/v1/admin/dashboard", null, null, HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.GET, "/api/v1/ai/safety-insights", null, null, HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.POST, "/api/v1/alerts", null, alertBody("anonymous"), HttpStatus.UNAUTHORIZED);

        HttpHeaders studentHeaders = bearer(studentToken);
        HttpHeaders officerHeaders = bearer(officerToken);
        HttpHeaders adminHeaders = bearer(adminToken);
        ResponseEntity<String> incidentResponse = http.postForEntity("/api/v1/incidents",
                new HttpEntity<>(Map.of("category", "SUSPICIOUS_ACTIVITY",
                        "description", "Authorization matrix incident " + suffix,
                        "locationName", "Test Campus", "severity", "LOW"), studentHeaders), String.class);
        assertThat(incidentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        long incidentId = mapper.readTree(incidentResponse.getBody()).get("id").asLong();
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/assign",
                null, Map.of("officerId", officer.getId()), HttpStatus.UNAUTHORIZED);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/status",
                null, Map.of("status", "ACKNOWLEDGED"), HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> studentIncidents = http.exchange("/api/v1/incidents", HttpMethod.GET,
                new HttpEntity<>(studentHeaders), String.class);
        assertThat(studentIncidents.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentIncidents.getBody()).contains("Authorization matrix incident " + suffix);
        assertStatus(HttpMethod.GET, "/api/v1/admin/dashboard", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/analytics", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/users", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/audit-logs", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/ai/safety-insights", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/officers", studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/admin/users/" + studentId + "/role?role=ADMIN",
                studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/assign",
                studentHeaders, Map.of("officerId", officer.getId()), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/status",
                studentHeaders, Map.of("status", "ACKNOWLEDGED"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/incidents/" + incidentId + "/notes",
                studentHeaders, Map.of("note", "Student cannot add responder note"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/locations", studentHeaders,
                locationBody("student-" + suffix), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/emergency-contacts", studentHeaders,
                contactBody("student-" + suffix), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/alerts", studentHeaders, alertBody("student"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/ai/summarize-incident", studentHeaders,
                Map.of("description", "Student must not use responder summaries"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/ai/classify-incident", studentHeaders,
                Map.of("description", "Student incident classification is allowed"), HttpStatus.OK);

        assertStatus(HttpMethod.GET, "/api/v1/admin/dashboard", officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/analytics", officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/users", officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/admin/audit-logs", officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.GET, "/api/v1/ai/safety-insights", officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/admin/users/" + studentId + "/role?role=ADMIN",
                officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/alerts", officerHeaders, alertBody("officer"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/emergency-contacts", officerHeaders,
                contactBody("officer-" + suffix), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.POST, "/api/v1/ai/summarize-incident", officerHeaders,
                Map.of("description", "Officer incident response summary"), HttpStatus.OK);

        ResponseEntity<String> officerList = http.exchange("/api/v1/officers", HttpMethod.GET,
                new HttpEntity<>(officerHeaders), String.class);
        assertThat(officerList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(officerList.getBody()).contains(officer.getEmail());
        assertStatus(HttpMethod.POST, "/api/v1/locations", officerHeaders,
                locationBody("officer-" + suffix), HttpStatus.OK);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/assign",
                officerHeaders, Map.of("officerId", officer.getId()), HttpStatus.OK);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/status",
                officerHeaders, Map.of("status", "ACKNOWLEDGED"), HttpStatus.OK);
        assertStatus(HttpMethod.POST, "/api/v1/incidents/" + incidentId + "/notes",
                officerHeaders, Map.of("note", "Officer-only incident response note"), HttpStatus.OK);
        assertStatus(HttpMethod.POST, "/api/v1/ai/summarize-incident", adminHeaders,
                Map.of("description", "Administrator incident response summary"), HttpStatus.OK);

        for (String endpoint : java.util.List.of("/api/v1/admin/dashboard", "/api/v1/admin/analytics",
                "/api/v1/admin/users", "/api/v1/admin/audit-logs", "/api/v1/ai/safety-insights")) {
            assertStatus(HttpMethod.GET, endpoint, adminHeaders, null, HttpStatus.OK);
        }
        assertStatus(HttpMethod.PATCH, "/api/v1/admin/users/" + studentId + "/role?role=SECURITY_OFFICER",
                adminHeaders, null, HttpStatus.OK);
        ResponseEntity<String> createdAlert = http.postForEntity("/api/v1/alerts",
                new HttpEntity<>(alertBody("admin-" + suffix), adminHeaders), String.class);
        assertThat(createdAlert.getStatusCode()).isEqualTo(HttpStatus.OK);
        long alertId = mapper.readTree(createdAlert.getBody()).get("id").asLong();
        assertStatus(HttpMethod.PATCH, "/api/v1/alerts/" + alertId, studentHeaders,
                alertBody("student-update"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.DELETE, "/api/v1/alerts/" + alertId, studentHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/alerts/" + alertId, officerHeaders,
                alertBody("officer-update"), HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.DELETE, "/api/v1/alerts/" + alertId, officerHeaders, null, HttpStatus.FORBIDDEN);
        assertStatus(HttpMethod.PATCH, "/api/v1/alerts/" + alertId, adminHeaders,
                alertBody("admin-updated-" + suffix), HttpStatus.OK);
        assertStatus(HttpMethod.DELETE, "/api/v1/alerts/" + alertId, adminHeaders, null, HttpStatus.NO_CONTENT);
        assertStatus(HttpMethod.POST, "/api/v1/emergency-contacts", adminHeaders,
                contactBody("admin-" + suffix), HttpStatus.OK);
        assertStatus(HttpMethod.POST, "/api/v1/locations", adminHeaders,
                locationBody("admin-" + suffix), HttpStatus.OK);
        assertStatus(HttpMethod.PATCH, "/api/v1/incidents/" + incidentId + "/status",
                adminHeaders, Map.of("status", "RESPONDING"), HttpStatus.OK);
        assertStatus(HttpMethod.POST, "/api/v1/incidents/" + incidentId + "/notes",
                adminHeaders, Map.of("note", "Administrator response note"), HttpStatus.OK);
    }

    @Test
    void adminCanManageIncidentWhileStudentIsDeniedAndActionIsAudited() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User administrator = new User("Campus", "Admin", "admin-" + suffix + "@example.edu",
                passwordEncoder.encode("CampusAdmin#123"), Role.ADMIN);
        administrator = userRepository.save(administrator);
        User officer = new User("Riley", "Officer", "officer-" + suffix + "@example.edu",
                passwordEncoder.encode("CampusOfficer#123"), Role.SECURITY_OFFICER);
        officer = userRepository.save(officer);
        String adminToken = login(administrator.getEmail(), "CampusAdmin#123");
        String officerToken = login(officer.getEmail(), "CampusOfficer#123");
        String studentToken = registerStudent(suffix);

        HttpHeaders studentHeaders = bearer(studentToken);
        ResponseEntity<String> created = http.postForEntity("/api/v1/incidents",
                new HttpEntity<>(Map.of("category", "SUSPICIOUS_ACTIVITY",
                        "description", "Unrecognized person is attempting to enter the lab.",
                        "locationName", "Research Lab 2", "severity", "HIGH"), studentHeaders), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        long id = mapper.readTree(created.getBody()).get("id").asLong();

        ResponseEntity<String> denied = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "ACKNOWLEDGED"), studentHeaders), String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        HttpHeaders adminHeaders = bearer(adminToken);
        ResponseEntity<String> assigned = http.exchange("/api/v1/incidents/" + id + "/assign", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("officerId", officer.getId()), adminHeaders),
                String.class);
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(assigned.getBody()).get("assignedOfficer").get("email").asText())
                .isEqualTo(officer.getEmail());

        HttpHeaders officerHeaders = bearer(officerToken);
        ResponseEntity<String> updated = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "ACKNOWLEDGED", "responseNotes", "Officer dispatched"), officerHeaders),
                String.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(updated.getBody()).get("status").asText()).isEqualTo("ACKNOWLEDGED");

        ResponseEntity<String> noted = http.postForEntity("/api/v1/incidents/" + id + "/notes",
                new HttpEntity<>(Map.of("note", "Arrived at the east entrance."), officerHeaders), String.class);
        assertThat(noted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(noted.getBody()).get("responseNotes").asText())
                .contains("Arrived at the east entrance.");

        ResponseEntity<String> regression = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "NEW"), officerHeaders), String.class);
        assertThat(regression.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> unchanged = http.exchange("/api/v1/incidents/" + id, HttpMethod.GET,
                new HttpEntity<>(officerHeaders), String.class);
        assertThat(mapper.readTree(unchanged.getBody()).get("status").asText()).isEqualTo("ACKNOWLEDGED");

        ResponseEntity<String> history = http.exchange("/api/v1/incidents/" + id + "/history", HttpMethod.GET,
                new HttpEntity<>(officerHeaders), String.class);
        assertThat(history.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(history.getBody()).contains("CREATED", "ASSIGNED", "STATUS_UPDATED", "NOTE_ADDED",
                "Status: NEW -> ACKNOWLEDGED");
        ResponseEntity<String> notifications = http.exchange("/api/v1/notifications", HttpMethod.GET,
                new HttpEntity<>(studentHeaders), String.class);
        assertThat(notifications.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(notifications.getBody()).toString()).contains("status changed");
        ResponseEntity<String> officerNotifications = http.exchange("/api/v1/notifications", HttpMethod.GET,
                new HttpEntity<>(officerHeaders), String.class);
        assertThat(officerNotifications.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(officerNotifications.getBody()).contains("was assigned to you");

        String otherStudentToken = registerStudent(suffix + "-other");
        JsonNode studentNotification = mapper.readTree(notifications.getBody()).get(0);
        ResponseEntity<String> crossUserNotificationUpdate = http.exchange(
                "/api/v1/notifications/" + studentNotification.get("id").asLong() + "/read",
                HttpMethod.PATCH, new HttpEntity<>(bearer(otherStudentToken)), String.class);
        assertThat(crossUserNotificationUpdate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> repeatedStatus = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "ACKNOWLEDGED"), officerHeaders), String.class);
        assertThat(repeatedStatus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<String> closed = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "CLOSED"), officerHeaders), String.class);
        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> reopened = http.exchange("/api/v1/incidents/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "RESOLVED"), officerHeaders), String.class);
        assertThat(reopened.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<String> studentAlertCreation = http.postForEntity("/api/v1/alerts",
                new HttpEntity<>(Map.of("title", "Unauthorized", "message", "Should be rejected",
                        "alertType", "WEATHER", "severity", "LOW", "affectedArea", "Campus",
                        "expiresAt", LocalDateTime.now().plusDays(1).toString()), studentHeaders), String.class);
        assertThat(studentAlertCreation.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> createdAlert = http.postForEntity("/api/v1/alerts",
                new HttpEntity<>(Map.of("title", "Test weather notice", "message", "Test-only alert",
                        "alertType", "WEATHER", "severity", "LOW", "affectedArea", "Test area",
                        "expiresAt", LocalDateTime.now().plusDays(1).toString()), adminHeaders), String.class);
        assertThat(createdAlert.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(createdAlert.getBody()).get("alertType").asText()).isEqualTo("WEATHER");
        ResponseEntity<String> activeAlerts = http.getForEntity("/api/v1/alerts", String.class);
        assertThat(activeAlerts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activeAlerts.getBody()).contains("Test weather notice", "WEATHER");
        ResponseEntity<String> alertedStudentNotifications = http.exchange("/api/v1/notifications", HttpMethod.GET,
                new HttpEntity<>(studentHeaders), String.class);
        assertThat(alertedStudentNotifications.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(alertedStudentNotifications.getBody()).contains("Campus alert: Test weather notice");

        ResponseEntity<String> studentContactCreation = http.postForEntity("/api/v1/emergency-contacts",
                new HttpEntity<>(Map.of("name", "Test Contact", "role", "Test", "phone", "555-0123",
                        "priority", 1), studentHeaders), String.class);
        assertThat(studentContactCreation.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> createdContact = http.postForEntity("/api/v1/emergency-contacts",
                new HttpEntity<>(Map.of("name", "Test Contact", "role", "Campus Safety",
                        "phone", "555-0123", "priority", 1), adminHeaders), String.class);
        assertThat(createdContact.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> publicContacts = http.getForEntity("/api/v1/emergency-contacts", String.class);
        assertThat(publicContacts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicContacts.getBody()).contains("Test Contact");

        ResponseEntity<String> auditLogs = http.exchange("/api/v1/admin/audit-logs", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(auditLogs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditLogs.getBody()).contains(studentEmailFrom(suffix), administrator.getEmail(),
                officer.getEmail(), "STATUS_UPDATED", "ASSIGNED");
        ResponseEntity<String> studentAuditLogs = http.exchange("/api/v1/admin/audit-logs", HttpMethod.GET,
                new HttpEntity<>(studentHeaders), String.class);
        assertThat(studentAuditLogs.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<String> studentDashboard = http.exchange("/api/v1/admin/dashboard", HttpMethod.GET,
                new HttpEntity<>(studentHeaders), String.class);
        assertThat(studentDashboard.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> analytics = http.exchange("/api/v1/admin/dashboard", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);
        JsonNode dashboard = mapper.readTree(analytics.getBody());
        assertThat(analytics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.get("totalIncidents").asLong()).isGreaterThan(0);
        assertThat(dashboard.get("resolvedIncidents").asLong()).isEqualTo(1);
    }

    private String registerStudent(String suffix) throws Exception {
        ResponseEntity<String> response = http.postForEntity("/api/v1/auth/register",
                new HttpEntity<>(Map.of("firstName", "Pat", "lastName", "Student",
                        "email", "student-" + suffix + "@example.edu", "password", "CampusTest#123"), jsonHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("token").asText();
    }

    private String studentEmailFrom(String suffix) {
        return "student-" + suffix + "@example.edu";
    }

    private void assertStatus(HttpMethod method, String path, HttpHeaders headers, Object body,
                             HttpStatus expectedStatus) {
        HttpHeaders requestHeaders = headers == null ? jsonHeaders() : headers;
        ResponseEntity<String> response = http.exchange(path, method,
                new HttpEntity<>(body, requestHeaders), String.class);
        assertThat(response.getStatusCode()).as("%s %s", method, path).isEqualTo(expectedStatus);
    }

    private Map<String, Object> alertBody(String suffix) {
        return Map.of("title", "Role matrix alert " + suffix, "message", "Test alert message",
                "alertType", "WEATHER", "severity", "LOW", "affectedArea", "Test Campus",
                "expiresAt", LocalDateTime.now().plusDays(1).toString());
    }

    private Map<String, Object> contactBody(String suffix) {
        return Map.of("name", "Role Matrix Contact " + suffix, "role", "Campus Safety",
                "phone", "555-0123", "priority", 10);
    }

    private Map<String, Object> locationBody(String suffix) {
        return Map.of("name", "Role Matrix Location " + suffix, "code", "RM-" + suffix,
                "description", "Test-only location");
    }

    private String login(String email, String password) throws Exception {
        ResponseEntity<String> response = http.postForEntity("/api/v1/auth/login",
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders()), String.class);
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
