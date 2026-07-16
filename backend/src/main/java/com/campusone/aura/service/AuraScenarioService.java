package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraScenarioDtos;
import com.campusone.aura.dto.AuraScenarioDtos.EmergencyRepairResponse;
import com.campusone.aura.dto.AuraScenarioDtos.WhatIfResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraScenarioRepository;
import com.campusone.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraScenarioService {

    private static final Set<String> WHAT_IF_TYPES = Set.of(
            "ROOM_UNAVAILABLE", "INSTRUCTOR_UNAVAILABLE", "SECTION_ADDED",
            "OFFERING_ADDED", "ENROLLMENT_CHANGED", "TIMESLOT_REMOVED",
            "LAB_DURATION_CHANGED", "INSTRUCTOR_REPLACED", "REGISTRATION_ADDED",
            "TRAVEL_RULE_CHANGED", "EXCEPTION_ADDED", "FACILITY_REMOVED");
    private static final Set<String> EMERGENCY_TYPES = Set.of(
            "ROOM_CLOSURE", "INSTRUCTOR_ABSENCE", "UNIVERSITY_EVENT",
            "TIMESLOT_CANCELLATION", "FACILITY_OUTAGE", "SECTION_RESTRICTION");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository repository;
    private final AuraScenarioRepository scenarioRepository;
    private final AuraClashDetector clashDetector;

    public AuraScenarioService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository repository,
            AuraScenarioRepository scenarioRepository,
            AuraClashDetector clashDetector) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.scenarioRepository = scenarioRepository;
        this.clashDetector = clashDetector;
    }

    @Transactional
    public WhatIfResponse runWhatIf(
            UUID userId,
            UUID termId,
            AuraScenarioDtos.WhatIfRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTermAndVersion(termId, request.sourceVersionId(), universityId);
        String scenarioType = normalize(request.scenarioType(), WHAT_IF_TYPES,
                "Choose a supported what-if scenario.");
        UUID resourceId = optionalResourceId(request.scenarioInput());
        validateResourceScope(scenarioType, resourceId, universityId);
        int affected = scenarioRepository.countAffectedSessions(
                request.sourceVersionId(), scenarioType, resourceId);
        String recommendation = affected == 0
                ? "The current timetable has no directly affected sessions. Review readiness before applying this input change."
                : affected + " session" + (affected == 1 ? " is" : "s are")
                        + " directly affected. Create a repair draft before changing live scheduling data.";
        UUID id = scenarioRepository.insertCompletedWhatIf(
                UUID.randomUUID(), universityId, termId,
                request.sourceVersionId(), userId, scenarioType,
                request.scenarioInput(), affected, recommendation);
        repository.insertVersionAudit(
                request.sourceVersionId(), userId, "WHAT_IF_COMPLETED",
                "Completed a non-destructive " + scenarioType + " simulation.");
        return scenarioRepository.findWhatIf(id)
                .orElseThrow(() -> notFound("AURA what-if result was not found."));
    }

    public List<WhatIfResponse> listWhatIf(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTerm(termId, universityId);
        return scenarioRepository.listWhatIf(termId);
    }

    @Transactional
    public EmergencyRepairResponse createEmergencyDraft(
            UUID userId,
            UUID termId,
            AuraScenarioDtos.EmergencyRepairRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTermAndVersion(termId, request.sourceVersionId(), universityId);
        if (!scenarioRepository.isPublishedVersionForTerm(
                request.sourceVersionId(), termId)) {
            throw new AuraStateException(
                    "Emergency repairs must start from the published timetable.");
        }
        String emergencyType = normalize(
                request.emergencyType(), EMERGENCY_TYPES,
                "Choose a supported emergency type.");
        validateResourceScope(emergencyType, request.affectedResourceId(), universityId);
        int affected = scenarioRepository.countAffectedSessions(
                request.sourceVersionId(), emergencyType,
                request.affectedResourceId());
        UUID requestId = scenarioRepository.insertEmergency(
                UUID.randomUUID(), universityId, termId,
                request.sourceVersionId(), userId, emergencyType,
                request.affectedResourceId(), request.reason(), affected);
        UUID draftId = repository.cloneVersion(
                UUID.randomUUID(), request.sourceVersionId(),
                repository.nextVersionNumber(termId),
                "Emergency repair: " + request.reason().trim(), userId,
                "EMERGENCY_REPAIR");
        scenarioRepository.completeEmergencyDraft(
                requestId, draftId, emergencyType, request.affectedResourceId());
        List<DetectedClash> clashes = clashDetector.detect(
                repository.listSessions(draftId));
        repository.replaceClashes(draftId, clashes);
        repository.insertVersionAudit(
                draftId, userId, "EMERGENCY_DRAFT_CREATED",
                "Created a minimally scoped emergency repair draft with unaffected sessions pinned.");
        return scenarioRepository.findEmergency(requestId)
                .orElseThrow(() -> notFound("AURA emergency repair was not found."));
    }

    public List<EmergencyRepairResponse> listEmergencies(
            UUID userId,
            UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTerm(termId, universityId);
        return scenarioRepository.listEmergencies(termId);
    }

    private void requireTermAndVersion(
            UUID termId,
            UUID versionId,
            UUID universityId) {
        requireTerm(termId, universityId);
        if (!repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, versionId, universityId)) {
            throw notFound("AURA timetable version was not found.");
        }
        TimetableVersionResponse version = repository.findVersion(versionId)
                .orElseThrow(() -> notFound("AURA timetable version was not found."));
        if (!termId.equals(version.termId())) {
            throw notFound("AURA timetable version was not found.");
        }
    }

    private void requireTerm(UUID termId, UUID universityId) {
        if (!repository.resourceBelongsToUniversity(
                ScopedResource.TERM, termId, universityId)) {
            throw notFound("AURA term was not found.");
        }
    }

    private void validateResourceScope(
            String type,
            UUID resourceId,
            UUID universityId) {
        ScopedResource resource = switch (type) {
            case "ROOM_UNAVAILABLE", "ROOM_CLOSURE", "FACILITY_REMOVED", "FACILITY_OUTAGE" -> ScopedResource.ROOM;
            case "INSTRUCTOR_UNAVAILABLE", "INSTRUCTOR_ABSENCE", "INSTRUCTOR_REPLACED" -> ScopedResource.INSTRUCTOR;
            case "TIMESLOT_REMOVED", "TIMESLOT_CANCELLATION", "UNIVERSITY_EVENT" -> ScopedResource.TIMESLOT;
            case "SECTION_RESTRICTION", "SECTION_ADDED" -> ScopedResource.SECTION;
            default -> null;
        };
        if (resource != null && resourceId == null) {
            throw new AuraStateException("Choose the affected scheduling resource.");
        }
        if (resource != null && !repository.resourceBelongsToUniversity(
                resource, resourceId, universityId)) {
            throw notFound("Affected scheduling resource was not found.");
        }
    }

    private UUID optionalResourceId(Map<String, Object> input) {
        Object raw = input.get("resourceId");
        if (raw == null || raw.toString().isBlank()) return null;
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException exception) {
            throw new AuraStateException("Choose a valid affected scheduling resource.");
        }
    }

    private String normalize(String value, Set<String> supported, String message) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!supported.contains(normalized)) throw new AuraStateException(message);
        return normalized;
    }

    private ResourceNotFoundException notFound(String message) {
        String suffix = " was not found.";
        return new ResourceNotFoundException(message.endsWith(suffix)
                ? message.substring(0, message.length() - suffix.length())
                : message);
    }
}
