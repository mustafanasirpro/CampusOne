package com.campusone.moderation.repository;

import com.campusone.moderation.entity.ModerationAction;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationTargetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModerationActionRepository
        extends JpaRepository<ModerationAction, UUID> {

    @EntityGraph(attributePaths = {
        "moderator",
        "moderator.studentProfile",
        "report"
    })
    @Query("""
            select action
            from ModerationAction action
            where (
                    :actionType is null
                    or action.actionType = :actionType
              )
              and (
                    :targetType is null
                    or action.targetType = :targetType
              )
              and (
                    :moderatorUserId is null
                    or action.moderator.id = :moderatorUserId
              )
              and (
                    :reportId is null
                    or action.report.id = :reportId
              )
            """)
    Page<ModerationAction> findForHistory(
            @Param("actionType") ModerationActionType actionType,
            @Param("targetType") ModerationTargetType targetType,
            @Param("moderatorUserId") UUID moderatorUserId,
            @Param("reportId") UUID reportId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "moderator",
        "moderator.studentProfile",
        "report"
    })
    @Query("""
            select action
            from ModerationAction action
            where action.id = :actionId
            """)
    Optional<ModerationAction> findDetailedById(
            @Param("actionId") UUID actionId);
}
