package com.campusone.moderation.repository;

import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentReportRepository
        extends JpaRepository<ContentReport, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusInAndDeletedFalse(
            UUID reporterUserId,
            ModerationTargetType targetType,
            UUID targetId,
            Collection<ReportStatus> statuses);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select report
            from ContentReport report
            where report.reporter.id = :reporterUserId
              and report.deleted = false
              and (:status is null or report.status = :status)
              and (
                    :targetType is null
                    or report.targetType = :targetType
              )
            """)
    Page<ContentReport> findOwnedReports(
            @Param("reporterUserId") UUID reporterUserId,
            @Param("status") ReportStatus status,
            @Param("targetType") ModerationTargetType targetType,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select report
            from ContentReport report
            where report.id = :reportId
              and report.reporter.id = :reporterUserId
              and report.deleted = false
            """)
    Optional<ContentReport> findOwnedActiveById(
            @Param("reporterUserId") UUID reporterUserId,
            @Param("reportId") UUID reportId);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select report
            from ContentReport report
            where report.id = :reportId
              and report.deleted = false
            """)
    Optional<ContentReport> findActiveById(
            @Param("reportId") UUID reportId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select report
            from ContentReport report
            where report.id = :reportId
              and report.deleted = false
            """)
    Optional<ContentReport> findActiveByIdForUpdate(
            @Param("reportId") UUID reportId);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select report
            from ContentReport report
            where report.deleted = false
              and (:status is null or report.status = :status)
              and (:reason is null or report.reason = :reason)
              and (
                    :targetType is null
                    or report.targetType = :targetType
              )
              and (
                    :reporterUserId is null
                    or report.reporter.id = :reporterUserId
              )
            """)
    Page<ContentReport> findForModeration(
            @Param("status") ReportStatus status,
            @Param("reason") ReportReason reason,
            @Param("targetType") ModerationTargetType targetType,
            @Param("reporterUserId") UUID reporterUserId,
            Pageable pageable);
}
