package com.campusone.discussion;

import static org.mockito.Mockito.mock;

import com.campusone.discussion.repository.DiscussionAnswerRepository;
import com.campusone.discussion.repository.DiscussionAnswerVoteRepository;
import com.campusone.discussion.repository.DiscussionQuestionRepository;
import com.campusone.discussion.repository.DiscussionQuestionVoteRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
class DiscussionTestRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(DiscussionQuestionRepository.class)
    DiscussionQuestionRepository discussionQuestionRepository() {
        return mock(DiscussionQuestionRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(DiscussionAnswerRepository.class)
    DiscussionAnswerRepository discussionAnswerRepository() {
        return mock(DiscussionAnswerRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(DiscussionQuestionVoteRepository.class)
    DiscussionQuestionVoteRepository discussionQuestionVoteRepository() {
        return mock(DiscussionQuestionVoteRepository.class);
    }

    @Bean
    @ConditionalOnMissingBean(DiscussionAnswerVoteRepository.class)
    DiscussionAnswerVoteRepository discussionAnswerVoteRepository() {
        return mock(DiscussionAnswerVoteRepository.class);
    }
}
