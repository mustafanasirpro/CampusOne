package com.campusone.search;

import static org.mockito.Mockito.mock;

import com.campusone.search.repository.GlobalSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class SearchTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalSearchRepository.class)
    GlobalSearchRepository globalSearchRepository() {
        return mock(GlobalSearchRepository.class);
    }
}
