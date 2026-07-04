package com.campusone.moderation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.moderation.dto.request.CreateReportRequest;
import com.campusone.moderation.dto.request.DismissReportRequest;
import com.campusone.moderation.dto.request.ResolveReportRequest;
import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportPageResponse;
import com.campusone.moderation.dto.response.ModerationActionPageResponse;
import com.campusone.moderation.dto.response.ModerationActionResponse;
import com.campusone.moderation.dto.response.ModeratorStatusResponse;
import com.campusone.moderation.dto.response.ModeratorSummaryResponse;
import com.campusone.moderation.dto.response.ReporterSummaryResponse;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.exception.ModeratorAccessDeniedException;
import com.campusone.moderation.service.ModerationAdminService;
import com.campusone.moderation.service.ModerationReportService;
import com.campusone.moderation.service.ModeratorAuthorizationService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.entity.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@WebMvcTest({
    ModerationReportController.class,
    ModerationAdminController.class
})
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class ModerationControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID REPORT_ID = UUID.fromString(
            "b1000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID ACTION_ID = UUID.fromString(
            "b2000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationReportService reportService;

    @MockitoBean
    private ModerationAdminService adminService;

    @MockitoBean
    private ModeratorAuthorizationService authorizationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "student@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
    }

    @Test
    void allModerationEndpoints_withoutAuthentication_areUnauthorized()
            throws Exception {
        assertUnauthorized(post("/api/v1/moderation/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validReportJson()));
        assertUnauthorized(get("/api/v1/moderation/reports/my"));
        assertUnauthorized(get(
                "/api/v1/moderation/reports/my/{reportId}",
                REPORT_ID));
        assertUnauthorized(get("/api/v1/admin/moderation/me"));
        assertUnauthorized(get("/api/v1/admin/moderation/reports"));
        assertUnauthorized(get(
                "/api/v1/admin/moderation/reports/{reportId}",
                REPORT_ID));
        assertUnauthorized(patch(
                "/api/v1/admin/moderation/reports/{reportId}/review",
                REPORT_ID));
        assertUnauthorized(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/resolve",
                        REPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validResolutionJson()));
        assertUnauthorized(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/dismiss",
                        REPORT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validResolutionJson()));
        assertUnauthorized(get("/api/v1/admin/moderation/actions"));
        assertUnauthorized(get(
                "/api/v1/admin/moderation/actions/{actionId}",
                ACTION_ID));
    }

    @Test
    void createReport_validRequest_returnsCreated() throws Exception {
        when(reportService.createReport(
                eq(USER_ID),
                any(CreateReportRequest.class)))
                .thenReturn(detail(ReportStatus.PENDING));

        mockMvc.perform(post("/api/v1/moderation/reports")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validReportJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/moderation/reports/my/"
                                + REPORT_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReport_invalidRequest_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/moderation/reports")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "details": "%s"
                                }
                                """.formatted("x".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void listMyReports_filtersStatusAndTargetType()
            throws Exception {
        when(reportService.listMyReports(
                USER_ID,
                ReportStatus.PENDING,
                ModerationTargetType.NOTE,
                0,
                20,
                ModerationSort.NEWEST))
                .thenReturn(emptyReportPage());

        mockMvc.perform(get("/api/v1/moderation/reports/my")
                        .with(authentication(authentication))
                        .param("status", "PENDING")
                        .param("targetType", "NOTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getMyReport_returnsOwnedDetail() throws Exception {
        when(reportService.getMyReport(USER_ID, REPORT_ID))
                .thenReturn(detail(ReportStatus.PENDING));

        mockMvc.perform(get(
                        "/api/v1/moderation/reports/my/{reportId}",
                        REPORT_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(REPORT_ID.toString()));
    }

    @Test
    void moderatorStatus_normalUser_returnsInactive()
            throws Exception {
        when(authorizationService.getStatus(USER_ID))
                .thenReturn(new ModeratorStatusResponse(
                        false,
                        null,
                        null,
                        null));

        mockMvc.perform(get("/api/v1/admin/moderation/me")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeModerator")
                        .value(false));
    }

    @Test
    void moderatorStatus_activeModerator_returnsRole()
            throws Exception {
        when(authorizationService.getStatus(USER_ID))
                .thenReturn(new ModeratorStatusResponse(
                        true,
                        ModeratorRole.MODERATOR,
                        NOW,
                        null));

        mockMvc.perform(get("/api/v1/admin/moderation/me")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeModerator")
                        .value(true))
                .andExpect(jsonPath("$.role")
                        .value("MODERATOR"));
    }

    @Test
    void adminListReports_activeModerator_returnsFilteredPage()
            throws Exception {
        when(adminService.listReports(
                USER_ID,
                ReportStatus.PENDING,
                ReportReason.SPAM,
                ModerationTargetType.NOTE,
                USER_ID,
                0,
                20,
                ModerationSort.NEWEST))
                .thenReturn(emptyReportPage());

        mockMvc.perform(get("/api/v1/admin/moderation/reports")
                        .with(authentication(authentication))
                        .param("status", "PENDING")
                        .param("reason", "SPAM")
                        .param("targetType", "NOTE")
                        .param("reporterUserId", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void adminListReports_normalUser_isForbidden()
            throws Exception {
        when(adminService.listReports(
                eq(USER_ID),
                any(),
                any(),
                any(),
                any(),
                eq(0),
                eq(20),
                eq(ModerationSort.NEWEST)))
                .thenThrow(new ModeratorAccessDeniedException());

        mockMvc.perform(get("/api/v1/admin/moderation/reports")
                        .with(authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("ACCESS_DENIED"));
    }

    @Test
    void adminGetReport_activeModerator_returnsDetail()
            throws Exception {
        when(adminService.getReport(USER_ID, REPORT_ID))
                .thenReturn(detail(ReportStatus.PENDING));

        mockMvc.perform(get(
                        "/api/v1/admin/moderation/reports/{reportId}",
                        REPORT_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(REPORT_ID.toString()));
    }

    @Test
    void reviewReport_activeModerator_returnsUnderReview()
            throws Exception {
        when(adminService.reviewReport(USER_ID, REPORT_ID))
                .thenReturn(detail(ReportStatus.UNDER_REVIEW));

        mockMvc.perform(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/review",
                        REPORT_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("UNDER_REVIEW"));
    }

    @Test
    void resolveAndDismiss_validRequests_returnTerminalStates()
            throws Exception {
        when(adminService.resolveReport(
                eq(USER_ID),
                eq(REPORT_ID),
                any(ResolveReportRequest.class)))
                .thenReturn(detail(ReportStatus.RESOLVED));
        when(adminService.dismissReport(
                eq(USER_ID),
                eq(REPORT_ID),
                any(DismissReportRequest.class)))
                .thenReturn(detail(ReportStatus.DISMISSED));

        mockMvc.perform(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/resolve",
                        REPORT_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validResolutionJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("RESOLVED"));
        mockMvc.perform(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/dismiss",
                        REPORT_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validResolutionJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("DISMISSED"));
    }

    @Test
    void resolveReport_invalidNote_returnsValidationError()
            throws Exception {
        mockMvc.perform(patch(
                        "/api/v1/admin/moderation/reports/{reportId}/resolve",
                        REPORT_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resolutionNote": "x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void moderationActionListAndDetail_returnResponses()
            throws Exception {
        when(adminService.listActions(
                USER_ID,
                ModerationActionType.REPORT_REVIEWED,
                ModerationTargetType.NOTE,
                USER_ID,
                REPORT_ID,
                0,
                20,
                ModerationSort.NEWEST))
                .thenReturn(emptyActionPage());
        when(adminService.getAction(USER_ID, ACTION_ID))
                .thenReturn(action());

        mockMvc.perform(get("/api/v1/admin/moderation/actions")
                        .with(authentication(authentication))
                        .param("actionType", "REPORT_REVIEWED")
                        .param("targetType", "NOTE")
                        .param("moderatorUserId", USER_ID.toString())
                        .param("reportId", REPORT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get(
                        "/api/v1/admin/moderation/actions/{actionId}",
                        ACTION_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(ACTION_ID.toString()));
    }

    @Test
    void invalidQueryEnum_returnsMalformedRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation/reports")
                        .with(authentication(authentication))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"));
    }

    private void assertUnauthorized(RequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    private String validReportJson() {
        return """
                {
                  "targetType": "NOTE",
                  "targetId": "%s",
                  "reason": "SPAM",
                  "details": "Repeated promotional content."
                }
                """.formatted(TARGET_ID);
    }

    private String validResolutionJson() {
        return """
                {"resolutionNote": "Reviewed by the moderation team."}
                """;
    }

    private ContentReportDetailResponse detail(
            ReportStatus status) {
        return new ContentReportDetailResponse(
                REPORT_ID,
                new ReporterSummaryResponse(USER_ID, "Ayesha Malik"),
                ModerationTargetType.NOTE,
                TARGET_ID,
                ReportReason.SPAM,
                "Repeated promotional content.",
                status,
                status == ReportStatus.PENDING
                        ? null
                        : new ModeratorSummaryResponse(
                                USER_ID,
                                "Moderator"),
                status == ReportStatus.PENDING ? null : NOW,
                status == ReportStatus.RESOLVED
                                || status == ReportStatus.DISMISSED
                        ? "Reviewed by the moderation team."
                        : null,
                NOW,
                NOW);
    }

    private ContentReportPageResponse emptyReportPage() {
        return new ContentReportPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private ModerationActionPageResponse emptyActionPage() {
        return new ModerationActionPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private ModerationActionResponse action() {
        return new ModerationActionResponse(
                ACTION_ID,
                new ModeratorSummaryResponse(USER_ID, "Moderator"),
                REPORT_ID,
                ModerationActionType.REPORT_REVIEWED,
                ModerationTargetType.NOTE,
                TARGET_ID,
                "Report moved to under review.",
                null,
                NOW);
    }
}
