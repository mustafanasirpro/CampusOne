package com.campusone.moderation.service;

import com.campusone.common.service.CommunityIntegrationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.moderation.dto.request.CreateReportRequest;
import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.exception.DuplicateActiveReportException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ContentReportRepository;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ModerationReportServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID REPORT_ID = UUID.fromString(
            "b1000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private ContentReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private LostFoundItemRepository lostFoundItemRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    private ModerationReportService service;
    private User reporter;
    private ContentReport report;

    @BeforeEach
    void setUp() {
        reporter = user(USER_ID);
        report = report(reporter);
        service = new ModerationReportService(
                reportRepository,
                userRepository,
                moderatorRepository,
                lostFoundItemRepository,
                new ModerationMapper(),
                integrationService);
    }

    @Test
    void createReport_validRequest_createsPendingReport() {
        when(reportRepository
                .existsByReporterIdAndTargetTypeAndTargetIdAndStatusInAndDeletedFalse(
                        eq(USER_ID),
                        eq(ModerationTargetType.NOTE),
                        eq(TARGET_ID),
                        any()))
                .thenReturn(false);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(reporter));
        when(reportRepository.saveAndFlush(any(ContentReport.class)))
                .thenAnswer(invocation ->
                        persist(invocation.getArgument(0)));

        var response = service.createReport(
                USER_ID,
                createRequest());

        assertThat(response.id()).isEqualTo(REPORT_ID);
        assertThat(response.status())
                .isEqualTo(ReportStatus.PENDING);
        assertThat(response.reporter().userId())
                .isEqualTo(USER_ID);
    }

    @Test
    void createReport_duplicateActiveReport_isRejected() {
        when(reportRepository
                .existsByReporterIdAndTargetTypeAndTargetIdAndStatusInAndDeletedFalse(
                        eq(USER_ID),
                        eq(ModerationTargetType.NOTE),
                        eq(TARGET_ID),
                        any()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.createReport(USER_ID, createRequest()))
                .isInstanceOf(
                        DuplicateActiveReportException.class);
    }

    @Test
    void listMyReports_filtersAndReturnsPagination() {
        when(reportRepository.findOwnedReports(
                eq(USER_ID),
                eq(ReportStatus.PENDING),
                eq(ModerationTargetType.NOTE),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(report),
                        PageRequest.of(1, 10),
                        21));

        var response = service.listMyReports(
                USER_ID,
                ReportStatus.PENDING,
                ModerationTargetType.NOTE,
                1,
                10,
                ModerationSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
        verify(reportRepository).findOwnedReports(
                eq(USER_ID),
                eq(ReportStatus.PENDING),
                eq(ModerationTargetType.NOTE),
                any(Pageable.class));
    }

    @Test
    void getMyReport_ownedReport_returnsDetail() {
        when(reportRepository.findOwnedActiveById(
                USER_ID,
                REPORT_ID))
                .thenReturn(Optional.of(report));

        var response = service.getMyReport(USER_ID, REPORT_ID);

        assertThat(response.id()).isEqualTo(REPORT_ID);
    }

    @Test
    void getMyReport_otherUsersReport_returnsNotFound() {
        when(reportRepository.findOwnedActiveById(
                OTHER_USER_ID,
                REPORT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getMyReport(OTHER_USER_ID, REPORT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CreateReportRequest createRequest() {
        return new CreateReportRequest(
                ModerationTargetType.NOTE,
                TARGET_ID,
                ReportReason.SPAM,
                "Repeated promotional content.");
    }

    private User user(UUID id) {
        User value = new User(
                "student@example.com",
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private ContentReport report(User owner) {
        return persist(new ContentReport(
                owner,
                ModerationTargetType.NOTE,
                TARGET_ID,
                ReportReason.SPAM,
                "Repeated promotional content."));
    }

    private ContentReport persist(ContentReport value) {
        ReflectionTestUtils.setField(value, "id", REPORT_ID);
        ReflectionTestUtils.setField(value, "createdAt", NOW);
        ReflectionTestUtils.setField(value, "updatedAt", NOW);
        return value;
    }
}
