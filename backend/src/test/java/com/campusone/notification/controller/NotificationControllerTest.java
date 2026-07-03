package com.campusone.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.notification.dto.request.CreateNotificationRequest;
import com.campusone.notification.dto.request.NotificationSort;
import com.campusone.notification.dto.response.NotificationBulkActionResponse;
import com.campusone.notification.dto.response.NotificationDetailResponse;
import com.campusone.notification.dto.response.NotificationPageResponse;
import com.campusone.notification.dto.response.NotificationUnreadCountResponse;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
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

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class NotificationControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString(
            "70000000-0000-4000-8000-000000000001");
    private static final UUID TARGET_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

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
    void notificationEndpoints_withoutAuthentication_areUnauthorized()
            throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(
                        "/api/v1/notifications/{notificationId}",
                        NOTIFICATION_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/read",
                        NOTIFICATION_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/unread",
                        NOTIFICATION_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(
                        "/api/v1/notifications/{notificationId}",
                        NOTIFICATION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createNotification_withAuthentication_returnsCreated()
            throws Exception {
        when(notificationService.createSelfNotification(
                eq(USER_ID),
                any(CreateNotificationRequest.class)))
                .thenReturn(detailResponse(false));

        mockMvc.perform(post("/api/v1/notifications")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/notifications/" + NOTIFICATION_ID))
                .andExpect(jsonPath("$.id")
                        .value(NOTIFICATION_ID.toString()))
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void createNotification_invalidRequest_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "USER_REMINDER",
                                  "title": "No",
                                  "message": " ",
                                  "targetId": "%s"
                                }
                                """.formatted(TARGET_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createNotification_malformedType_returnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson().replace(
                                "\"USER_REMINDER\"",
                                "\"EMAIL\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listNotifications_withAuthentication_returnsPage()
            throws Exception {
        when(notificationService.listMyNotifications(
                USER_ID,
                true,
                NotificationType.EVENT_REMINDER,
                NotificationTargetType.EVENT,
                0,
                20,
                NotificationSort.NEWEST))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(authentication))
                        .param("unreadOnly", "true")
                        .param("type", "EVENT_REMINDER")
                        .param("targetType", "EVENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getNotification_withAuthentication_returnsDetail()
            throws Exception {
        when(notificationService.getNotification(
                USER_ID,
                NOTIFICATION_ID))
                .thenReturn(detailResponse(false));

        mockMvc.perform(get(
                        "/api/v1/notifications/{notificationId}",
                        NOTIFICATION_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(NOTIFICATION_ID.toString()));
    }

    @Test
    void unreadCount_withAuthentication_returnsCount()
            throws Exception {
        when(notificationService.getUnreadCount(USER_ID))
                .thenReturn(new NotificationUnreadCountResponse(5));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markRead_withAuthentication_returnsUpdatedNotification()
            throws Exception {
        when(notificationService.markRead(USER_ID, NOTIFICATION_ID))
                .thenReturn(detailResponse(true));

        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/read",
                        NOTIFICATION_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markUnread_withAuthentication_returnsUpdatedNotification()
            throws Exception {
        when(notificationService.markUnread(USER_ID, NOTIFICATION_ID))
                .thenReturn(detailResponse(false));

        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/unread",
                        NOTIFICATION_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(false));
    }

    @Test
    void markAllRead_withAuthentication_returnsUpdatedCount()
            throws Exception {
        when(notificationService.markAllRead(USER_ID))
                .thenReturn(new NotificationBulkActionResponse(3));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(3));
    }

    @Test
    void deleteNotification_withAuthentication_returnsNoContent()
            throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/notifications/{notificationId}",
                        NOTIFICATION_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());
    }

    private NotificationPageResponse emptyPage() {
        return new NotificationPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private NotificationDetailResponse detailResponse(boolean read) {
        return new NotificationDetailResponse(
                NOTIFICATION_ID,
                NotificationType.EVENT_REMINDER,
                "AI workshop starts tomorrow",
                "Your registered AI workshop starts tomorrow at 10 AM.",
                NotificationTargetType.EVENT,
                TARGET_ID,
                "/events/" + TARGET_ID,
                read,
                read ? NOW : null,
                NOW,
                NOW);
    }

    private String validCreateJson() {
        return """
                {
                  "type": "USER_REMINDER",
                  "title": "Review database normalization",
                  "message": "Review the third normal form examples before class.",
                  "targetType": "NOTE",
                  "targetId": "%s",
                  "actionUrl": "/notes/%s"
                }
                """.formatted(TARGET_ID, TARGET_ID);
    }
}
