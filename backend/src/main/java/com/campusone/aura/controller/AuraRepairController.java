package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.service.AuraRepairService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/aura")
@Tag(name = "AURA Repair")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraRepairController {

    private final AuraRepairService service;

    public AuraRepairController(AuraRepairService service) {
        this.service = service;
    }

    @PostMapping("/versions/{versionId}/repair-preview")
    @Operation(summary = "Preview an automatic localized minimum-disruption repair")
    public ResponseEntity<AuraOperationsDtos.RepairPlanResponse> preview(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId,
            @Valid @RequestBody AuraOperationsDtos.RepairPreviewRequest request) {
        return ResponseEntity.ok(service.preview(principal.getUserId(), versionId, request));
    }

    @PostMapping("/repair-plans/{planId}/apply")
    @Operation(summary = "Atomically apply a current localized repair preview")
    public ResponseEntity<AuraOperationsDtos.RepairPlanResponse> apply(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID planId,
            @Valid @RequestBody AuraOperationsDtos.ApplyRepairRequest request) {
        return ResponseEntity.ok(service.apply(principal.getUserId(), planId, request));
    }

    @GetMapping("/repair-plans/{planId}")
    @Operation(summary = "Get a university-scoped repair plan")
    public ResponseEntity<AuraOperationsDtos.RepairPlanResponse> get(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID planId) {
        return ResponseEntity.ok(service.get(principal.getUserId(), planId));
    }
}
