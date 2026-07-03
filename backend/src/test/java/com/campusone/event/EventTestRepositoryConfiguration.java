package com.campusone.event;

import static org.mockito.Mockito.mock;

import com.campusone.event.repository.CampusEventRepository;
import com.campusone.event.repository.EventParticipantRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class EventTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(CampusEventRepository.class)
    CampusEventRepository campusEventRepository() {
        return mock(CampusEventRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(EventParticipantRepository.class)
    EventParticipantRepository eventParticipantRepository() {
        return mock(EventParticipantRepository.class);
    }
}
