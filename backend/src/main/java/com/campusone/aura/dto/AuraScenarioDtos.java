package com.campusone.aura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuraScenarioDtos {

    private AuraScenarioDtos() {
    }

    public record WhatIfRequest(
            @NotNull UUID sourceVersionId,
            @NotBlank @Size(max = 40) String scenarioType,
            @NotNull Map<String, Object> scenarioInput) {
    }

    public record WhatIfResponse(
            UUID id,
            UUID termId,
            UUID sourceVersionId,
            String scenarioType,
            String status,
            int affectedSessions,
            int clashesAdded,
            int clashesRemoved,
            String recommendation,
            Instant createdAt,
            Instant completedAt) {
    }

    public record EmergencyRepairRequest(
            @NotNull UUID sourceVersionId,
            @NotBlank @Size(max = 32) String emergencyType,
            @NotNull UUID affectedResourceId,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record EmergencyRepairResponse(
            UUID id,
            UUID termId,
            UUID sourceVersionId,
            UUID draftVersionId,
            String emergencyType,
            UUID affectedResourceId,
            String reason,
            String status,
            int affectedSessions,
            int reassignedSessions,
            String message,
            Instant createdAt) {
    }
}
