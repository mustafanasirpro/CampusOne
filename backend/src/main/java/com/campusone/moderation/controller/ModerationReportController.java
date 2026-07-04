package com.campusone.moderation.controller;

import com.campusone.moderation.dto.request.CreateReportRequest;
import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportPageResponse;
import com.campusone.moderation.entity.ModerationSort;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportStatus;
import com.campusone.moderation.service.ModerationReportService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/moderation/reports")
@Validated
@Tag(name = "Content Reports")
@SecurityRequirement(name = "bearerAuth")
public class ModerationReportController {

    private final ModerationReportService reportService;

    public ModerationReportController(
            ModerationReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Report a content reference")
    public ResponseEntity<ContentReportDetailResponse> createReport(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateReportRequest request) {
        ContentReportDetailResponse response =
                reportService.createReport(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/moderation/reports/my/"
                                        + response.id()))
                .body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's reports")
    public ResponseEntity<ContentReportPageResponse> listMyReports(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false)
            ModerationTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size,
            @RequestParam(defaultValue = "NEWEST")
            ModerationSort sort) {
        return ResponseEntity.ok(reportService.listMyReports(
                principal.getUserId(),
                status,
                targetType,
                page,
                size,
                sort));
    }

    @GetMapping("/my/{reportId}")
    @Operation(summary = "Get one report owned by the current user")
    public ResponseEntity<ContentReportDetailResponse> getMyReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(reportService.getMyReport(
                principal.getUserId(),
                reportId));
    }
}
