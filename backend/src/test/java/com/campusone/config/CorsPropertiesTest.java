package com.campusone.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowedOrigins_exactHttpOrigins_passValidation() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of(
                "https://app.campusone.pk",
                "http://localhost:5173"));

        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void allowedOrigins_wildcardOrPath_failsClosed() {
        CorsProperties wildcard = new CorsProperties();
        wildcard.setAllowedOrigins(List.of("https://*.campusone.pk"));
        CorsProperties path = new CorsProperties();
        path.setAllowedOrigins(List.of("https://campusone.pk/app"));

        assertThat(validator.validate(wildcard)).isNotEmpty();
        assertThat(validator.validate(path)).isNotEmpty();
    }

    @Test
    void allowedOrigins_missingOrBlank_usesSafeLoopbackDefaults() {
        CorsProperties missing = new CorsProperties();
        CorsProperties blank = new CorsProperties();
        blank.setAllowedOrigins(List.of("", "  "));

        assertThat(validator.validate(missing)).isEmpty();
        assertThat(validator.validate(blank)).isEmpty();
        assertThat(missing.getAllowedOrigins())
                .containsExactly(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173");
        assertThat(blank.getAllowedOrigins())
                .containsExactly(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173");
    }
}
