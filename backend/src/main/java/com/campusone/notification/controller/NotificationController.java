package com.campusone.notification.controller;

import com.campusone.notification.dto.request.CreateNotificationRequest;
import com.campusone.notification.dto.request.NotificationSort;
import com.campusone.notification.dto.response.NotificationBulkActionResponse;
import com.campusone.notification.dto.response.NotificationDetailResponse;
import com.campusone.notification.dto.response.NotificationPageResponse;
import com.campusone.notification.dto.response.NotificationUnreadCountResponse;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @Operation(summary = "Create a notification for the current user")
    public ResponseEntity<NotificationDetailResponse> createNotification(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationDetailResponse response =
                notificationService.createSelfNotification(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(
                        URI.create("/api/v1/notifications/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's notifications")
    public ResponseEntity<NotificationPageResponse> listNotifications(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false)
            NotificationTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") NotificationSort sort) {
        return ResponseEntity.ok(notificationService.listMyNotifications(
                principal.getUserId(),
                unreadOnly,
                type,
                targetType,
                page,
                size,
                sort));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the current user's unread notification count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(
                principal.getUserId()));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get an owned notification")
    public ResponseEntity<NotificationDetailResponse> getNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotification(
                principal.getUserId(),
                notificationId));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark an owned notification as read")
    public ResponseEntity<NotificationDetailResponse> markRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markRead(
                principal.getUserId(),
                notificationId));
    }

    @PatchMapping("/{notificationId}/unread")
    @Operation(summary = "Mark an owned notification as unread")
    public ResponseEntity<NotificationDetailResponse> markUnread(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markUnread(
                principal.getUserId(),
                notificationId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all current-user notifications as read")
    public ResponseEntity<NotificationBulkActionResponse> markAllRead(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAllRead(
                principal.getUserId()));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Soft-delete an owned notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        notificationService.deleteNotification(
                principal.getUserId(),
                notificationId);
        return ResponseEntity.noContent().build();
    }
}
