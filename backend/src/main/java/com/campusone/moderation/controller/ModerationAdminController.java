package com.campusone.moderation.controller;

import com.campusone.moderation.dto.request.DismissReportRequest;
import com.campusone.moderation.dto.request.ResolveReportRequest;
import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportPageResponse;
import com.campusone.moderation.dto.response.ModerationActionPageResponse;
import com.campusone.moderation.dto.response.ModerationActionResponse;
import com.campusone.moderation.dto.response.ModeratorStatusResponse;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.service.ModerationAdminService;
import com.campusone.moderation.service.ModeratorAuthorizationService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@Validated
@Tag(name = "Admin Moderation")
@SecurityRequirement(name = "bearerAuth")
public class ModerationAdminController {

    private final ModerationAdminService adminService;
    private final ModeratorAuthorizationService authorizationService;

    public ModerationAdminController(
            ModerationAdminService adminService,
            ModeratorAuthorizationService authorizationService) {
        this.adminService = adminService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's moderator status")
    public ResponseEntity<ModeratorStatusResponse> getModeratorStatus(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(authorizationService.getStatus(
                principal.getUserId()));
    }

    @GetMapping("/reports")
    @Operation(summary = "List reports for moderation")
    public ResponseEntity<ContentReportPageResponse> listReports(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(required = false)
            ModerationTargetType targetType,
            @RequestParam(required = false) UUID reporterUserId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size,
            @RequestParam(defaultValue = "NEWEST")
            ModerationSort sort) {
        return ResponseEntity.ok(adminService.listReports(
                principal.getUserId(),
                status,
                reason,
                targetType,
                reporterUserId,
                page,
                size,
                sort));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Get a report for moderation")
    public ResponseEntity<ContentReportDetailResponse> getReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(adminService.getReport(
                principal.getUserId(),
                reportId));
    }

    @PatchMapping("/reports/{reportId}/review")
    @Operation(summary = "Place a pending report under review")
    public ResponseEntity<ContentReportDetailResponse> reviewReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(adminService.reviewReport(
                principal.getUserId(),
                reportId));
    }

    @PatchMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Resolve a report")
    public ResponseEntity<ContentReportDetailResponse> resolveReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(adminService.resolveReport(
                principal.getUserId(),
                reportId,
                request));
    }

    @PatchMapping("/reports/{reportId}/dismiss")
    @Operation(summary = "Dismiss a report")
    public ResponseEntity<ContentReportDetailResponse> dismissReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody DismissReportRequest request) {
        return ResponseEntity.ok(adminService.dismissReport(
                principal.getUserId(),
                reportId,
                request));
    }

    @GetMapping("/actions")
    @Operation(summary = "List moderation action history")
    public ResponseEntity<ModerationActionPageResponse> listActions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false)
            ModerationActionType actionType,
            @RequestParam(required = false)
            ModerationTargetType targetType,
            @RequestParam(required = false) UUID moderatorUserId,
            @RequestParam(required = false) UUID reportId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size,
            @RequestParam(defaultValue = "NEWEST")
            ModerationSort sort) {
        return ResponseEntity.ok(adminService.listActions(
                principal.getUserId(),
                actionType,
                targetType,
                moderatorUserId,
                reportId,
                page,
                size,
                sort));
    }

    @GetMapping("/actions/{actionId}")
    @Operation(summary = "Get one moderation action")
    public ResponseEntity<ModerationActionResponse> getAction(
            @PathVariable UUID actionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(adminService.getAction(
                principal.getUserId(),
                actionId));
    }
}
