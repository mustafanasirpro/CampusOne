package com.campusone.notification;

import static org.mockito.Mockito.mock;

import com.campusone.notification.repository.NotificationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class NotificationTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationRepository.class)
    NotificationRepository notificationRepository() {
        return mock(NotificationRepository.class);
    }
}
