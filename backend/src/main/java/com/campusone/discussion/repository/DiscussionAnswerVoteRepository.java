package com.campusone.discussion.repository;

import com.campusone.discussion.entity.DiscussionAnswerVote;
import com.campusone.discussion.entity.DiscussionAnswerVoteId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscussionAnswerVoteRepository
        extends JpaRepository<DiscussionAnswerVote, DiscussionAnswerVoteId> {

    @Query("""
            select vote
            from DiscussionAnswerVote vote
            where vote.id.answerId in :answerIds
              and vote.id.userId = :userId
            """)
    List<DiscussionAnswerVote> findForAnswersAndUser(
            @Param("answerIds") List<UUID> answerIds,
            @Param("userId") UUID userId);
}
