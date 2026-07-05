package com.campusone.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class LocalCorsConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withPropertyValues("SPRING_PROFILES_ACTIVE=local")
                    .withUserConfiguration(CorsTestConfiguration.class);

    @Test
    void localProfile_withoutEnvironmentOverride_usesTrustedLoopbackOrigins() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CorsProperties.class).getAllowedOrigins())
                    .containsExactly(
                            "http://localhost:5173",
                            "http://127.0.0.1:5173");
        });
    }

    @Test
    void localProfile_withEnvironmentOverride_usesExplicitTrustedOrigins() {
        contextRunner
                .withPropertyValues(
                        "CORS_ALLOWED_ORIGINS=https://app.campusone.pk,"
                                + "https://admin.campusone.pk")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).getAllowedOrigins())
                            .containsExactly(
                                    "https://app.campusone.pk",
                                    "https://admin.campusone.pk");
                });
    }

    @Test
    void defaultProfile_withoutEnvironmentOverride_usesTrustedLoopbackOrigins() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(
                        "SPRING_PROFILES_ACTIVE=",
                        "CORS_ALLOWED_ORIGINS=")
                .withUserConfiguration(CorsTestConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).getAllowedOrigins())
                            .containsExactly(
                                    "http://localhost:5173",
                                    "http://127.0.0.1:5173");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsTestConfiguration {
    }
}
