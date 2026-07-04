package com.campusone.gamification;

import static org.mockito.Mockito.mock;

import com.campusone.gamification.repository.BadgeRepository;
import com.campusone.gamification.repository.GamificationProfileRepository;
import com.campusone.gamification.repository.UserBadgeRepository;
import com.campusone.gamification.repository.XpTransactionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class GamificationTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(GamificationProfileRepository.class)
    GamificationProfileRepository gamificationProfileRepository() {
        return mock(GamificationProfileRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(XpTransactionRepository.class)
    XpTransactionRepository xpTransactionRepository() {
        return mock(XpTransactionRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(BadgeRepository.class)
    BadgeRepository badgeRepository() {
        return mock(BadgeRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(UserBadgeRepository.class)
    UserBadgeRepository userBadgeRepository() {
        return mock(UserBadgeRepository.class);
    }
}
