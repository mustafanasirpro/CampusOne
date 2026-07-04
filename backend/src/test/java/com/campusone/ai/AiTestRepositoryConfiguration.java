package com.campusone.ai;

import static org.mockito.Mockito.mock;

import com.campusone.ai.repository.AiChatMessageRepository;
import com.campusone.ai.repository.AiChatSessionRepository;
import com.campusone.ai.repository.AiGeneratedItemRepository;
import com.campusone.ai.repository.AiUsageRecordRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class AiTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(AiChatSessionRepository.class)
    AiChatSessionRepository aiChatSessionRepository() {
        return mock(AiChatSessionRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(AiChatMessageRepository.class)
    AiChatMessageRepository aiChatMessageRepository() {
        return mock(AiChatMessageRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(AiGeneratedItemRepository.class)
    AiGeneratedItemRepository aiGeneratedItemRepository() {
        return mock(AiGeneratedItemRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(AiUsageRecordRepository.class)
    AiUsageRecordRepository aiUsageRecordRepository() {
        return mock(AiUsageRecordRepository.class);
    }
}
