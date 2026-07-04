package com.campusone.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.moderation.dto.request.DismissReportRequest;
import com.campusone.moderation.dto.request.ResolveReportRequest;
import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationAction;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.exception.InvalidReportStatusTransitionException;
import com.campusone.moderation.exception.ModeratorAccessDeniedException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ContentReportRepository;
import com.campusone.moderation.repository.ModerationActionRepository;
import com.campusone.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ModerationAdminServiceTest {

    private static final UUID MODERATOR_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000010");
    private static final UUID REPORTER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID REPORT_ID = UUID.fromString(
            "b1000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID ACTION_ID = UUID.fromString(
            "b2000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private ContentReportRepository reportRepository;

    @Mock
    private ModerationActionRepository actionRepository;

    @Mock
    private ModeratorAuthorizationService authorizationService;

    private ModerationAdminService service;
    private User moderatorUser;
    private Moderator moderator;
    private ContentReport report;

    @BeforeEach
    void setUp() {
        moderatorUser = user(
                MODERATOR_ID,
                "moderator@example.com");
        moderator = new Moderator(
                moderatorUser,
                ModeratorRole.MODERATOR,
                null);
        ReflectionTestUtils.setField(
                moderator,
                "userId",
                MODERATOR_ID);
        report = report(user(
                REPORTER_ID,
                "reporter@example.com"));
        service = new ModerationAdminService(
                reportRepository,
                actionRepository,
                authorizationService,
                new ModerationMapper(),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void listReports_activeModerator_appliesAllFilters() {
        allowModerator();
        when(reportRepository.findForModeration(
                eq(ReportStatus.PENDING),
                eq(ReportReason.SPAM),
                eq(ModerationTargetType.NOTE),
                eq(REPORTER_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(report),
                        PageRequest.of(0, 10),
                        1));

        var response = service.listReports(
                MODERATOR_ID,
                ReportStatus.PENDING,
                ReportReason.SPAM,
                ModerationTargetType.NOTE,
                REPORTER_ID,
                0,
                10,
                ModerationSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void listReports_normalUser_isRejectedBeforeQuery() {
        when(authorizationService.requireActiveModerator(
                MODERATOR_ID))
                .thenThrow(new ModeratorAccessDeniedException());

        assertThatThrownBy(() -> service.listReports(
                MODERATOR_ID,
                null,
                null,
                null,
                null,
                0,
                20,
                ModerationSort.NEWEST))
                .isInstanceOf(
                        ModeratorAccessDeniedException.class);
        verify(reportRepository, never()).findForModeration(
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void getReport_activeModerator_returnsDetail() {
        allowModerator();
        when(reportRepository.findActiveById(REPORT_ID))
                .thenReturn(Optional.of(report));

        var response = service.getReport(
                MODERATOR_ID,
                REPORT_ID);

        assertThat(response.id()).isEqualTo(REPORT_ID);
    }

    @Test
    void reviewReport_pendingReport_updatesAndRecordsAction() {
        allowModerator();
        lockReport();
        stubActionSave();

        var response = service.reviewReport(
                MODERATOR_ID,
                REPORT_ID);

        assertThat(response.status())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
        assertThat(response.reviewedAt()).isEqualTo(NOW);
        ArgumentCaptor<ModerationAction> captor =
                ArgumentCaptor.forClass(ModerationAction.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType())
                .isEqualTo(
                        ModerationActionType.REPORT_REVIEWED);
        assertThat(captor.getValue().getMetadata()
                .get("newStatus").asText())
                .isEqualTo("UNDER_REVIEW");
    }

    @Test
    void reviewReport_alreadyUnderReview_isIdempotent() {
        report.markUnderReview(moderatorUser, NOW.minusSeconds(10));
        allowModerator();
        lockReport();

        var response = service.reviewReport(
                MODERATOR_ID,
                REPORT_ID);

        assertThat(response.status())
                .isEqualTo(ReportStatus.UNDER_REVIEW);
        verify(actionRepository, never())
                .save(any(ModerationAction.class));
    }

    @Test
    void reviewReport_terminalReport_isRejected() {
        report.resolve(
                moderatorUser,
                NOW.minusSeconds(10),
                "Resolved earlier.");
        allowModerator();
        lockReport();

        assertThatThrownBy(() -> service.reviewReport(
                MODERATOR_ID,
                REPORT_ID))
                .isInstanceOf(
                        InvalidReportStatusTransitionException.class);
    }

    @Test
    void reviewReport_dismissedReport_isRejected() {
        report.dismiss(
                moderatorUser,
                NOW.minusSeconds(10),
                "Dismissed earlier.");
        allowModerator();
        lockReport();

        assertThatThrownBy(() -> service.reviewReport(
                MODERATOR_ID,
                REPORT_ID))
                .isInstanceOf(
                        InvalidReportStatusTransitionException.class);
    }

    @Test
    void resolveReport_pendingReport_updatesAndRecordsAction() {
        allowModerator();
        lockReport();
        stubActionSave();

        var response = service.resolveReport(
                MODERATOR_ID,
                REPORT_ID,
                new ResolveReportRequest(
                        "Confirmed policy violation."));

        assertThat(response.status())
                .isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.resolutionNote())
                .isEqualTo("Confirmed policy violation.");
        verifyAction(ModerationActionType.REPORT_RESOLVED);
    }

    @Test
    void resolveReport_alreadyResolved_isIdempotent() {
        report.resolve(
                moderatorUser,
                NOW.minusSeconds(10),
                "Original resolution.");
        allowModerator();
        lockReport();

        var response = service.resolveReport(
                MODERATOR_ID,
                REPORT_ID,
                new ResolveReportRequest("Another note."));

        assertThat(response.resolutionNote())
                .isEqualTo("Original resolution.");
        verify(actionRepository, never())
                .save(any(ModerationAction.class));
    }

    @Test
    void resolveReport_dismissedReport_isRejected() {
        report.dismiss(
                moderatorUser,
                NOW.minusSeconds(10),
                "Dismissed earlier.");
        allowModerator();
        lockReport();

        assertThatThrownBy(() -> service.resolveReport(
                MODERATOR_ID,
                REPORT_ID,
                new ResolveReportRequest("Resolve now.")))
                .isInstanceOf(
                        InvalidReportStatusTransitionException.class);
    }

    @Test
    void dismissReport_pendingReport_updatesAndRecordsAction() {
        allowModerator();
        lockReport();
        stubActionSave();

        var response = service.dismissReport(
                MODERATOR_ID,
                REPORT_ID,
                new DismissReportRequest(
                        "No policy violation found."));

        assertThat(response.status())
                .isEqualTo(ReportStatus.DISMISSED);
        assertThat(response.resolutionNote())
                .isEqualTo("No policy violation found.");
        verifyAction(ModerationActionType.REPORT_DISMISSED);
    }

    @Test
    void dismissReport_alreadyDismissed_isIdempotent() {
        report.dismiss(
                moderatorUser,
                NOW.minusSeconds(10),
                "Original dismissal.");
        allowModerator();
        lockReport();

        var response = service.dismissReport(
                MODERATOR_ID,
                REPORT_ID,
                new DismissReportRequest("Another note."));

        assertThat(response.resolutionNote())
                .isEqualTo("Original dismissal.");
        verify(actionRepository, never())
                .save(any(ModerationAction.class));
    }

    @Test
    void dismissReport_resolvedReport_isRejected() {
        report.resolve(
                moderatorUser,
                NOW.minusSeconds(10),
                "Resolved earlier.");
        allowModerator();
        lockReport();

        assertThatThrownBy(() -> service.dismissReport(
                MODERATOR_ID,
                REPORT_ID,
                new DismissReportRequest("Dismiss now.")))
                .isInstanceOf(
                        InvalidReportStatusTransitionException.class);
    }

    @Test
    void listActions_activeModerator_appliesFiltersAndPagination() {
        allowModerator();
        ModerationAction action = persistedAction();
        when(actionRepository.findForHistory(
                eq(ModerationActionType.REPORT_REVIEWED),
                eq(ModerationTargetType.NOTE),
                eq(MODERATOR_ID),
                eq(REPORT_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(action),
                        PageRequest.of(1, 10),
                        22));

        var response = service.listActions(
                MODERATOR_ID,
                ModerationActionType.REPORT_REVIEWED,
                ModerationTargetType.NOTE,
                MODERATOR_ID,
                REPORT_ID,
                1,
                10,
                ModerationSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void getAction_activeModerator_returnsAction() {
        allowModerator();
        when(actionRepository.findDetailedById(ACTION_ID))
                .thenReturn(Optional.of(persistedAction()));

        var response = service.getAction(
                MODERATOR_ID,
                ACTION_ID);

        assertThat(response.id()).isEqualTo(ACTION_ID);
        assertThat(response.reportId()).isEqualTo(REPORT_ID);
    }

    private void allowModerator() {
        when(authorizationService.requireActiveModerator(
                MODERATOR_ID))
                .thenReturn(moderator);
    }

    private void lockReport() {
        when(reportRepository.findActiveByIdForUpdate(REPORT_ID))
                .thenReturn(Optional.of(report));
    }

    private void stubActionSave() {
        when(actionRepository.save(any(ModerationAction.class)))
                .thenAnswer(invocation -> {
                    ModerationAction action = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            action,
                            "id",
                            ACTION_ID);
                    ReflectionTestUtils.setField(
                            action,
                            "createdAt",
                            NOW);
                    return action;
                });
    }

    private void verifyAction(ModerationActionType actionType) {
        verify(actionRepository).save(
                org.mockito.ArgumentMatchers.argThat(
                        action -> action.getActionType() == actionType));
    }

    private User user(UUID id, String email) {
        User value = new User(
                email,
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private ContentReport report(User reporter) {
        ContentReport value = new ContentReport(
                reporter,
                ModerationTargetType.NOTE,
                TARGET_ID,
                ReportReason.SPAM,
                "Repeated promotional content.");
        ReflectionTestUtils.setField(value, "id", REPORT_ID);
        ReflectionTestUtils.setField(value, "createdAt", NOW);
        ReflectionTestUtils.setField(value, "updatedAt", NOW);
        return value;
    }

    private ModerationAction persistedAction() {
        ModerationAction action = new ModerationAction(
                moderatorUser,
                report,
                ModerationActionType.REPORT_REVIEWED,
                ModerationTargetType.NOTE,
                TARGET_ID,
                "Report moved to under review.",
                null);
        ReflectionTestUtils.setField(action, "id", ACTION_ID);
        ReflectionTestUtils.setField(action, "createdAt", NOW);
        return action;
    }
}
