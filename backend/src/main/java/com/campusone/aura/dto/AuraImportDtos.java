package com.campusone.aura.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AuraImportDtos {

    private AuraImportDtos() {
    }

    public record ImportPreviewResponse(
            UUID id,
            UUID termId,
            String importType,
            String fileFormat,
            String originalFilename,
            String status,
            List<String> sources,
            String selectedSource,
            List<String> headers,
            List<Map<String, String>> rows,
            Map<String, String> suggestedMapping,
            int totalRows,
            boolean truncated,
            boolean ocrRequired,
            List<String> warnings,
            Instant createdAt) {
    }

    public record ValidateImportRequest(
            @NotEmpty Map<@Size(max = 60) String, @Size(max = 160) String> mapping,
            @Size(max = 120) String saveAsProfile) {
    }

    public record ImportRowIssue(
            int rowNumber,
            String field,
            String code,
            String message,
            String severity) {
    }

    public record ImportValidationResponse(
            UUID id,
            String status,
            Map<String, String> mapping,
            int acceptedRows,
            int rejectedRows,
            List<Map<String, String>> normalizedPreview,
            List<ImportRowIssue> issues) {
    }

    public record ImportApplyResponse(
            UUID id,
            String importType,
            String status,
            int acceptedRows,
            int rejectedRows,
            UUID resultVersionId,
            String message) {
    }
}
