package com.campusone.aura;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.CampusOneApplication;
import com.campusone.aura.controller.AuraAdminController;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.service.AuraAuthorizationService;
import com.campusone.aura.service.AuraClashDetector;
import com.campusone.aura.service.AuraReadinessValidator;
import com.campusone.aura.service.AuraService;
import com.campusone.aura.service.AuraSolverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = CampusOneApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.config.import=",
            "spring.profiles.active=",
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            "app.jwt.issuer=campusone-backend-startup-test",
            "app.jwt.audience=campusone-api-startup-test",
            "app.auth.cookie-secure=false",
            "app.auth.cookie-same-site=Strict",
            "app.password-reset.mail-provider=disabled",
            "app.storage.provider=disabled",
            "campusone.aura.enabled=true"
        })
class AuraDefaultProfileStartupIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuraAdminController auraAdminController;

    @Autowired
    private AuraService auraService;

    @Autowired
    private AuraAuthorizationService authorizationService;

    @Autowired
    private AuraJdbcRepository auraJdbcRepository;

    @Autowired
    private AuraSolverService auraSolverService;

    @Autowired
    private AuraReadinessValidator readinessValidator;

    @Autowired
    private AuraClashDetector clashDetector;

    @Test
    void defaultProfileStartsHttpServerAndLoadsAuraBeans() {
        assertThat(port).isPositive();
        assertThat(auraAdminController).isNotNull();
        assertThat(auraService).isNotNull();
        assertThat(authorizationService).isNotNull();
        assertThat(auraJdbcRepository).isNotNull();
        assertThat(auraSolverService).isNotNull();
        assertThat(readinessValidator).isNotNull();
        assertThat(clashDetector).isNotNull();

        String response = restTemplate.getForObject(
                "http://localhost:" + port + "/api/v1/health",
                String.class);

        assertThat(response).contains("\"status\":\"UP\"");
    }
}
