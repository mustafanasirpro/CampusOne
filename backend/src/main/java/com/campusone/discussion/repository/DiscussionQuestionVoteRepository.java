package com.campusone.discussion.repository;

import com.campusone.discussion.entity.DiscussionQuestionVote;
import com.campusone.discussion.entity.DiscussionQuestionVoteId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscussionQuestionVoteRepository
        extends JpaRepository<DiscussionQuestionVote, DiscussionQuestionVoteId> {
}
