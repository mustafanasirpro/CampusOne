package com.campusone.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.common.controller.ApiDocumentationAliasController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class OpenApiDeploymentConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withPropertyValues(
                            "SPRING_PROFILES_ACTIVE=",
                            "spring.config.import=")
                    .withUserConfiguration(OpenApiTestConfiguration.class);

    @Test
    void defaultProfile_exposesOpenApiAndDocumentationAliases() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment()
                            .getProperty("springdoc.api-docs.enabled", Boolean.class))
                    .isTrue();
            assertThat(context.getEnvironment()
                            .getProperty("springdoc.swagger-ui.enabled", Boolean.class))
                    .isTrue();
            assertThat(context).hasSingleBean(ApiDocumentationAliasController.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ApiDocumentationAliasController.class)
    static class OpenApiTestConfiguration {
    }
}
