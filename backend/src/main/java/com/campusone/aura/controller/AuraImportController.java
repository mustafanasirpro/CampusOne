package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraImportDtos.ImportPreviewResponse;
import com.campusone.aura.dto.AuraImportDtos.ImportApplyResponse;
import com.campusone.aura.dto.AuraImportDtos.ImportValidationResponse;
import com.campusone.aura.dto.AuraImportDtos.ValidateImportRequest;
import com.campusone.aura.service.AuraImportService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/aura")
@Tag(name = "AURA imports")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraImportController {

    private final AuraImportService importService;

    public AuraImportController(AuraImportService importService) {
        this.importService = importService;
    }

    @PostMapping(
            path = "/terms/{termId}/imports/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate and preview an AURA CSV, spreadsheet, or PDF import")
    public ResponseEntity<ImportPreviewResponse> preview(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @RequestParam String importType,
            @RequestParam(required = false) String source,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(importService.preview(
                principal.getUserId(), termId, importType, source, file));
    }

    @PostMapping("/imports/{jobId}/validate")
    @Operation(summary = "Validate an AURA import mapping and every source row")
    public ResponseEntity<ImportValidationResponse> validate(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID jobId,
            @Valid @RequestBody ValidateImportRequest request) {
        return ResponseEntity.ok(importService.validate(
                principal.getUserId(), jobId, request));
    }

    @PostMapping("/imports/{jobId}/apply")
    @Operation(summary = "Apply a fully validated AURA import atomically")
    public ResponseEntity<ImportApplyResponse> apply(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(importService.apply(principal.getUserId(), jobId));
    }
}
