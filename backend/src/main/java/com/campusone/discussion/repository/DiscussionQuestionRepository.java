package com.campusone.discussion.repository;

import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.entity.DiscussionQuestion;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscussionQuestionRepository
        extends JpaRepository<DiscussionQuestion, UUID> {

    @EntityGraph(attributePaths = {
        "author",
        "author.studentProfile",
        "author.studentProfile.university",
        "acceptedAnswer"
    })
    @Query("""
            select question
            from DiscussionQuestion question
            where question.deleted = false
              and question.status <> :hiddenStatus
              and (:category is null or question.category = :category)
              and (
                    :searchPattern is null
                    or lower(question.title) like :searchPattern escape '\\'
                    or lower(question.body) like :searchPattern escape '\\'
              )
            """)
    Page<DiscussionQuestion> findVisibleQuestions(
            @Param("hiddenStatus") DiscussionQuestionStatus hiddenStatus,
            @Param("category") DiscussionCategory category,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "author",
        "author.studentProfile",
        "author.studentProfile.university",
        "acceptedAnswer"
    })
    @Query("""
            select question
            from DiscussionQuestion question
            where question.deleted = false
              and question.author.id = :authorUserId
            """)
    Page<DiscussionQuestion> findMyQuestions(
            @Param("authorUserId") UUID authorUserId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "author",
        "author.studentProfile",
        "author.studentProfile.university",
        "acceptedAnswer"
    })
    @Query("""
            select question
            from DiscussionQuestion question
            where question.id = :questionId
              and question.deleted = false
            """)
    Optional<DiscussionQuestion> findActiveById(
            @Param("questionId") UUID questionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select question
            from DiscussionQuestion question
            where question.id = :questionId
              and question.deleted = false
            """)
    Optional<DiscussionQuestion> findActiveByIdForUpdate(
            @Param("questionId") UUID questionId);
}
