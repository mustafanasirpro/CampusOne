package com.campusone.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.campusone.aura.controller.AuraAdminController;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.service.AuraAuthorizationService;
import com.campusone.aura.service.AuraClashDetector;
import com.campusone.aura.service.AuraReadinessValidator;
import com.campusone.aura.service.AuraService;
import com.campusone.aura.service.AuraSolverService;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.notification.service.NotificationService;
import com.campusone.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class AuraModuleBeanWiringTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withBean(NamedParameterJdbcTemplate.class,
                            () -> mock(NamedParameterJdbcTemplate.class))
                    .withBean(UserRepository.class,
                            () -> mock(UserRepository.class))
                    .withBean(NoteAdminAuthorizationService.class,
                            () -> mock(NoteAdminAuthorizationService.class))
                    .withBean(ModeratorRepository.class,
                            () -> mock(ModeratorRepository.class))
                    .withBean(NotificationService.class,
                            () -> mock(NotificationService.class))
                    .withBean(ObjectMapper.class, ObjectMapper::new)
                    .withBean(Clock.class, Clock::systemUTC)
                    .withUserConfiguration(AuraModuleTestConfiguration.class);

    @Test
    void defaultConfiguration_wiresAuraRuntimeBeansTogether() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuraAdminController.class);
            assertThat(context).hasSingleBean(AuraService.class);
            assertThat(context).hasSingleBean(AuraAuthorizationService.class);
            assertThat(context).hasSingleBean(AuraJdbcRepository.class);
            assertThat(context).hasSingleBean(AuraReadinessValidator.class);
            assertThat(context).hasSingleBean(AuraSolverService.class);
            assertThat(context).hasSingleBean(AuraClashDetector.class);
        });
    }

    @Test
    void disabledConfiguration_disablesJdbcBackedAuraRuntimeBeans() {
        contextRunner
                .withPropertyValues("campusone.aura.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AuraAdminController.class);
                    assertThat(context).doesNotHaveBean(AuraService.class);
                    assertThat(context).doesNotHaveBean(AuraJdbcRepository.class);
                    assertThat(context).doesNotHaveBean(AuraReadinessValidator.class);
                    assertThat(context).hasSingleBean(AuraAuthorizationService.class);
                    assertThat(context).hasSingleBean(AuraSolverService.class);
                    assertThat(context).hasSingleBean(AuraClashDetector.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.campusone.aura")
    static class AuraModuleTestConfiguration {
    }
}
