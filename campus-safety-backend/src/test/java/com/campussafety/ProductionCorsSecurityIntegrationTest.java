package com.campussafety;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.frontend-url=https://campus-shield-pi.vercel.app")
class ProductionCorsSecurityIntegrationTest {

    private static final String FRONTEND_ORIGIN = "https://campus-shield-pi.vercel.app";

    @Autowired
    private TestRestTemplate http;

    @Test
    void productionOriginCanPreflightLoginAndProtectedApiWithoutJwt() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(FRONTEND_ORIGIN);
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("authorization", "content-type"));

        ResponseEntity<Void> loginPreflight = http.exchange(
                "/api/v1/auth/login", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

        assertThat(loginPreflight.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginPreflight.getHeaders().getAccessControlAllowOrigin()).isEqualTo(FRONTEND_ORIGIN);
        assertThat(loginPreflight.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.POST);
        assertThat(loginPreflight.getHeaders().getAccessControlAllowHeaders())
                .contains("authorization", "content-type");
        assertThat(loginPreflight.getHeaders().getAccessControlAllowCredentials()).isFalse();

        headers.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<Void> protectedPreflight = http.exchange(
                "/api/v1/incidents", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);
        assertThat(protectedPreflight.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(protectedPreflight.getHeaders().getAccessControlAllowOrigin()).isEqualTo(FRONTEND_ORIGIN);

        ResponseEntity<String> protectedRequest = http.getForEntity("/api/v1/incidents", String.class);
        assertThat(protectedRequest.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
