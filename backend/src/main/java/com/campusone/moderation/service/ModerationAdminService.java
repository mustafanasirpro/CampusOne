package com.campusone.moderation.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.moderation.dto.request.DismissReportRequest;
import com.campusone.moderation.dto.request.ResolveReportRequest;
import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportPageResponse;
import com.campusone.moderation.dto.response.ContentReportSummaryResponse;
import com.campusone.moderation.dto.response.ModerationActionPageResponse;
import com.campusone.moderation.dto.response.ModerationActionResponse;
import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationAction;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.exception.InvalidReportStatusTransitionException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ContentReportRepository;
import com.campusone.moderation.repository.ModerationActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationAdminService {

    private final ContentReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final ModeratorAuthorizationService authorizationService;
    private final ModerationMapper moderationMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ModerationAdminService(
            ContentReportRepository reportRepository,
            ModerationActionRepository actionRepository,
            ModeratorAuthorizationService authorizationService,
            ModerationMapper moderationMapper,
            ObjectMapper objectMapper,
            Clock clock) {
        this.reportRepository = reportRepository;
        this.actionRepository = actionRepository;
        this.authorizationService = authorizationService;
        this.moderationMapper = moderationMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ContentReportPageResponse listReports(
            UUID moderatorUserId,
            ReportStatus status,
            ReportReason reason,
            ModerationTargetType targetType,
            UUID reporterUserId,
            int page,
            int size,
            ModerationSort sort) {
        authorizationService.requireActiveModerator(moderatorUserId);
        Page<ContentReport> reports =
                reportRepository.findForModeration(
                        status,
                        reason,
                        targetType,
                        reporterUserId,
                        PageRequest.of(page, size, sort.toSort()));
        List<ContentReportSummaryResponse> content =
                reports.getContent().stream()
                        .map(moderationMapper::toReportSummary)
                        .toList();
        return new ContentReportPageResponse(
                content,
                reports.getNumber(),
                reports.getSize(),
                reports.getTotalElements(),
                reports.getTotalPages(),
                reports.isFirst(),
                reports.isLast());
    }

    @Transactional(readOnly = true)
    public ContentReportDetailResponse getReport(
            UUID moderatorUserId,
            UUID reportId) {
        authorizationService.requireActiveModerator(moderatorUserId);
        return moderationMapper.toReportDetail(
                requireReport(reportId));
    }

    @Transactional
    public ContentReportDetailResponse reviewReport(
            UUID moderatorUserId,
            UUID reportId) {
        Moderator moderator =
                authorizationService.requireActiveModerator(
                        moderatorUserId);
        ContentReport report = requireReportForUpdate(reportId);
        if (report.getStatus() == ReportStatus.UNDER_REVIEW) {
            return moderationMapper.toReportDetail(report);
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw invalidTransition(
                    "Only pending reports can be placed under review.");
        }

        ReportStatus previousStatus = report.getStatus();
        report.markUnderReview(
                moderator.getUser(),
                clock.instant());
        createAction(
                moderator,
                report,
                ModerationActionType.REPORT_REVIEWED,
                "Report moved to under review.",
                previousStatus,
                ReportStatus.UNDER_REVIEW);
        return moderationMapper.toReportDetail(report);
    }

    @Transactional
    public ContentReportDetailResponse resolveReport(
            UUID moderatorUserId,
            UUID reportId,
            ResolveReportRequest request) {
        Moderator moderator =
                authorizationService.requireActiveModerator(
                        moderatorUserId);
        ContentReport report = requireReportForUpdate(reportId);
        if (report.getStatus() == ReportStatus.RESOLVED) {
            return moderationMapper.toReportDetail(report);
        }
        if (report.getStatus() == ReportStatus.DISMISSED) {
            throw invalidTransition(
                    "A dismissed report cannot be resolved.");
        }

        ReportStatus previousStatus = report.getStatus();
        report.resolve(
                moderator.getUser(),
                clock.instant(),
                request.resolutionNote());
        createAction(
                moderator,
                report,
                ModerationActionType.REPORT_RESOLVED,
                request.resolutionNote(),
                previousStatus,
                ReportStatus.RESOLVED);
        return moderationMapper.toReportDetail(report);
    }

    @Transactional
    public ContentReportDetailResponse dismissReport(
            UUID moderatorUserId,
            UUID reportId,
            DismissReportRequest request) {
        Moderator moderator =
                authorizationService.requireActiveModerator(
                        moderatorUserId);
        ContentReport report = requireReportForUpdate(reportId);
        if (report.getStatus() == ReportStatus.DISMISSED) {
            return moderationMapper.toReportDetail(report);
        }
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw invalidTransition(
                    "A resolved report cannot be dismissed.");
        }

        ReportStatus previousStatus = report.getStatus();
        report.dismiss(
                moderator.getUser(),
                clock.instant(),
                request.resolutionNote());
        createAction(
                moderator,
                report,
                ModerationActionType.REPORT_DISMISSED,
                request.resolutionNote(),
                previousStatus,
                ReportStatus.DISMISSED);
        return moderationMapper.toReportDetail(report);
    }

    @Transactional(readOnly = true)
    public ModerationActionPageResponse listActions(
            UUID moderatorUserId,
            ModerationActionType actionType,
            ModerationTargetType targetType,
            UUID actionModeratorUserId,
            UUID reportId,
            int page,
            int size,
            ModerationSort sort) {
        authorizationService.requireActiveModerator(moderatorUserId);
        Page<ModerationAction> actions =
                actionRepository.findForHistory(
                        actionType,
                        targetType,
                        actionModeratorUserId,
                        reportId,
                        PageRequest.of(
                                page,
                                size,
                                sort.toSort()));
        return new ModerationActionPageResponse(
                actions.getContent().stream()
                        .map(moderationMapper::toAction)
                        .toList(),
                actions.getNumber(),
                actions.getSize(),
                actions.getTotalElements(),
                actions.getTotalPages(),
                actions.isFirst(),
                actions.isLast());
    }

    @Transactional(readOnly = true)
    public ModerationActionResponse getAction(
            UUID moderatorUserId,
            UUID actionId) {
        authorizationService.requireActiveModerator(moderatorUserId);
        ModerationAction action =
                actionRepository.findDetailedById(actionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Moderation action"));
        return moderationMapper.toAction(action);
    }

    private ContentReport requireReport(UUID reportId) {
        return reportRepository.findActiveById(reportId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Content report"));
    }

    private ContentReport requireReportForUpdate(UUID reportId) {
        return reportRepository.findActiveByIdForUpdate(reportId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Content report"));
    }

    private void createAction(
            Moderator moderator,
            ContentReport report,
            ModerationActionType actionType,
            String reason,
            ReportStatus previousStatus,
            ReportStatus newStatus) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("previousStatus", previousStatus.name());
        metadata.put("newStatus", newStatus.name());
        actionRepository.save(new ModerationAction(
                moderator.getUser(),
                report,
                actionType,
                report.getTargetType(),
                report.getTargetId(),
                reason,
                metadata));
    }

    private InvalidReportStatusTransitionException invalidTransition(
            String message) {
        return new InvalidReportStatusTransitionException(message);
    }
}
