package com.campusone.moderation.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.moderation.dto.request.CreateReportRequest;
import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportPageResponse;
import com.campusone.moderation.dto.response.ContentReportSummaryResponse;
import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.exception.DuplicateActiveReportException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ContentReportRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationReportService {

    private static final EnumSet<ReportStatus> ACTIVE_STATUSES =
            EnumSet.of(
                    ReportStatus.PENDING,
                    ReportStatus.UNDER_REVIEW);

    private final ContentReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ModerationMapper moderationMapper;

    public ModerationReportService(
            ContentReportRepository reportRepository,
            UserRepository userRepository,
            ModerationMapper moderationMapper) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.moderationMapper = moderationMapper;
    }

    @Transactional
    public ContentReportDetailResponse createReport(
            UUID userId,
            CreateReportRequest request) {
        if (reportRepository
                .existsByReporterIdAndTargetTypeAndTargetIdAndStatusInAndDeletedFalse(
                        userId,
                        request.targetType(),
                        request.targetId(),
                        ACTIVE_STATUSES)) {
            throw new DuplicateActiveReportException();
        }

        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        ContentReport report = new ContentReport(
                reporter,
                request.targetType(),
                request.targetId(),
                request.reason(),
                request.details());
        try {
            return moderationMapper.toReportDetail(
                    reportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateActiveReportException();
        }
    }

    @Transactional(readOnly = true)
    public ContentReportPageResponse listMyReports(
            UUID userId,
            ReportStatus status,
            ModerationTargetType targetType,
            int page,
            int size,
            ModerationSort sort) {
        Page<ContentReport> reports =
                reportRepository.findOwnedReports(
                        userId,
                        status,
                        targetType,
                        PageRequest.of(
                                page,
                                size,
                                sort.toSort()));
        List<ContentReportSummaryResponse> content =
                reports.getContent().stream()
                        .map(moderationMapper::toReportSummary)
                        .toList();
        return pageResponse(reports, content);
    }

    @Transactional(readOnly = true)
    public ContentReportDetailResponse getMyReport(
            UUID userId,
            UUID reportId) {
        ContentReport report =
                reportRepository.findOwnedActiveById(
                                userId,
                                reportId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Content report"));
        return moderationMapper.toReportDetail(report);
    }

    private ContentReportPageResponse pageResponse(
            Page<ContentReport> reports,
            List<ContentReportSummaryResponse> content) {
        return new ContentReportPageResponse(
                content,
                reports.getNumber(),
                reports.getSize(),
                reports.getTotalElements(),
                reports.getTotalPages(),
                reports.isFirst(),
                reports.isLast());
    }
}
