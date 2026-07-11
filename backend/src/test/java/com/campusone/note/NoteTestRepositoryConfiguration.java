package com.campusone.note;

import static org.mockito.Mockito.mock;

import com.campusone.note.repository.NoteSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class NoteTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(NoteSearchRepository.class)
    NoteSearchRepository noteSearchRepository() {
        return mock(NoteSearchRepository.class);
    }
}
