package com.campusone.internship;

import static org.mockito.Mockito.mock;

import com.campusone.internship.repository.InternshipRepository;
import com.campusone.internship.repository.SavedInternshipRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class InternshipTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(InternshipRepository.class)
    InternshipRepository internshipRepository() {
        return mock(InternshipRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(SavedInternshipRepository.class)
    SavedInternshipRepository savedInternshipRepository() {
        return mock(SavedInternshipRepository.class);
    }
}
