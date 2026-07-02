package com.campusone.discussion.repository;

import com.campusone.discussion.entity.DiscussionAnswer;
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

public interface DiscussionAnswerRepository
        extends JpaRepository<DiscussionAnswer, UUID> {

    @EntityGraph(attributePaths = {
        "author",
        "author.studentProfile",
        "author.studentProfile.university"
    })
    @Query("""
            select answer
            from DiscussionAnswer answer
            where answer.question.id = :questionId
              and answer.deleted = false
              and answer.question.deleted = false
            """)
    Page<DiscussionAnswer> findVisibleByQuestionId(
            @Param("questionId") UUID questionId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "question",
        "author",
        "author.studentProfile",
        "author.studentProfile.university"
    })
    @Query("""
            select answer
            from DiscussionAnswer answer
            where answer.id = :answerId
              and answer.deleted = false
              and answer.question.deleted = false
            """)
    Optional<DiscussionAnswer> findActiveById(
            @Param("answerId") UUID answerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select answer
            from DiscussionAnswer answer
            where answer.id = :answerId
              and answer.question.id = :questionId
              and answer.deleted = false
              and answer.question.deleted = false
            """)
    Optional<DiscussionAnswer> findActiveByIdAndQuestionIdForUpdate(
            @Param("answerId") UUID answerId,
            @Param("questionId") UUID questionId);
}
