package com.campusone.aura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuraResolutionDtos {

    private AuraResolutionDtos() {
    }

    public record CreateResolutionCaseRequest(
            @NotNull UUID termId,
            @NotNull UUID registrationId,
            @NotBlank @Size(max = 32) String caseType,
            @NotBlank @Size(max = 500) String summary) {
    }

    public record ResolutionDecisionRequest(
            UUID suggestionId,
            @NotBlank @Size(max = 500) String reason,
            long version) {
    }

    public record ResolutionSuggestionResponse(
            UUID id,
            String suggestionType,
            UUID targetOfferingId,
            String targetOfferingCode,
            String targetCourseTitle,
            UUID targetSectionId,
            String targetSectionName,
            UUID targetGroupId,
            String targetGroupType,
            String targetGroupCode,
            int rankOrder,
            boolean safe,
            int hardClashesRemoved,
            int hardClashesAdded,
            int affectedStudents,
            int changedSessions,
            String explanation,
            Instant appliedAt) {
    }

    public record ResolutionCaseResponse(
            UUID id,
            UUID termId,
            UUID studentUserId,
            String studentName,
            UUID registrationId,
            String status,
            String caseType,
            String summary,
            String reviewReason,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<ResolutionSuggestionResponse> suggestions) {
    }
}
