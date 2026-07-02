package com.campusone.marketplace;

import static org.mockito.Mockito.mock;

import com.campusone.marketplace.repository.MarketplaceListingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class MarketplaceTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(MarketplaceListingRepository.class)
    MarketplaceListingRepository marketplaceListingRepository() {
        return mock(MarketplaceListingRepository.class);
    }
}
