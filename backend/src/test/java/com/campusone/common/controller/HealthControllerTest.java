package com.campusone.common.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void health_whenRequested_returnsSuccessResponse() {
        ResponseEntity<Map<String, String>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("status", "UP")
                .containsEntry("service", "campusone-backend");
    }
}
