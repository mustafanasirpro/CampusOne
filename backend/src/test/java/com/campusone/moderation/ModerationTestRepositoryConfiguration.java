package com.campusone.moderation;

import static org.mockito.Mockito.mock;

import com.campusone.moderation.repository.ContentReportRepository;
import com.campusone.moderation.repository.ModerationActionRepository;
import com.campusone.moderation.repository.ModeratorRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class ModerationTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ModeratorRepository.class)
    ModeratorRepository moderatorRepository() {
        return mock(ModeratorRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(ContentReportRepository.class)
    ContentReportRepository contentReportRepository() {
        return mock(ContentReportRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(ModerationActionRepository.class)
    ModerationActionRepository moderationActionRepository() {
        return mock(ModerationActionRepository.class);
    }
}
