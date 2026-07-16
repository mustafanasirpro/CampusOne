package com.campusone.aura.service;

import com.campusone.aura.dto.AuraResolutionDtos;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionCaseResponse;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionDecisionRequest;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraResolutionRepository;
import com.campusone.aura.repository.AuraResolutionRepository.OfferingCandidate;
import com.campusone.aura.repository.AuraResolutionRepository.GroupCandidate;
import com.campusone.aura.repository.AuraResolutionRepository.RegistrationScope;
import com.campusone.aura.repository.AuraResolutionRepository.SuggestionTarget;
import com.campusone.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
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
public class AuraResolutionService {

    private static final Set<String> CASE_TYPES = Set.of(
            "REPEATER_CLASH",
            "ELECTIVE_CLASH",
            "CROSS_SECTION_CLASH",
            "SHARED_STUDENT_CLASH",
            "MANUAL_REQUEST");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository auraRepository;
    private final AuraResolutionRepository repository;
    private final AuraNotificationService notificationService;

    public AuraResolutionService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository auraRepository,
            AuraResolutionRepository repository,
            AuraNotificationService notificationService) {
        this.authorizationService = authorizationService;
        this.auraRepository = auraRepository;
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ResolutionCaseResponse requestResolution(
            UUID userId,
            AuraResolutionDtos.CreateResolutionCaseRequest request) {
        UUID universityId = authorizationService.requireUniversity(userId);
        if (!auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, request.termId(), universityId)) {
            throw notFound("AURA term was not found.");
        }
        RegistrationScope registration = repository
                .findRegistrationScope(request.registrationId())
                .orElseThrow(() -> notFound("AURA registration was not found."));
        if (!registration.studentUserId().equals(userId)
                || !registration.universityId().equals(universityId)
                || !registration.termId().equals(request.termId())) {
            throw notFound("AURA registration was not found.");
        }
        String caseType = normalizeCaseType(request.caseType());
        UUID caseId;
        try {
            caseId = repository.insertCase(
                    UUID.randomUUID(), registration, caseType,
                    request.summary(), userId);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new AuraStateException(
                    "A resolution request is already open for this registration.");
        }
        notificationService.notifyUniversityAdmins(
                universityId,
                userId,
                "New timetable resolution request",
                "A student timetable clash is ready for review.",
                caseId);
        return requireCase(caseId);
    }

    public List<ResolutionCaseResponse> listMyCases(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireUniversity(userId);
        requireTerm(termId, universityId);
        return repository.listCases(termId, userId);
    }

    public List<ResolutionCaseResponse> listAdminCases(UUID userId, UUID termId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        requireTerm(termId, universityId);
        return repository.listCases(termId, null);
    }

    @Transactional
    public ResolutionCaseResponse analyze(UUID userId, UUID caseId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ResolutionCaseResponse resolutionCase = requireAdminCase(caseId, universityId);
        if (!Set.of("OPEN", "SUGGESTED").contains(resolutionCase.status())) {
            throw new AuraStateException(
                    "Only open or suggested resolution cases can be analyzed.");
        }
        RegistrationScope registration = repository
                .findRegistrationScope(resolutionCase.registrationId())
                .orElseThrow(() -> notFound("AURA registration was not found."));
        List<OfferingCandidate> candidates = repository
                .parallelOfferingCandidates(registration);
        List<GroupCandidate> groupCandidates = repository
                .alternateGroupCandidates(registration);
        repository.replaceSuggestions(caseId, candidates, groupCandidates);
        if ("OPEN".equals(resolutionCase.status())) {
            boolean noAlternatives = candidates.isEmpty() && groupCandidates.isEmpty();
            String next = noAlternatives ? "FAILED" : "SUGGESTED";
            if (!repository.updateCaseStatus(
                    caseId, resolutionCase.version(), "OPEN", next,
                    userId, noAlternatives
                            ? "No compatible offering or teaching group is currently available."
                            : "Ranked safe student-only alternatives were generated.")) {
                throw conflict();
            }
        }
        repository.insertAction(
                caseId, null, userId, "ANALYZED",
                "Generated ranked student-only transfer alternatives.");
        return requireCase(caseId);
    }

    @Transactional
    public ResolutionCaseResponse approve(
            UUID userId,
            UUID caseId,
            ResolutionDecisionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ResolutionCaseResponse resolutionCase = requireAdminCase(caseId, universityId);
        SuggestionTarget suggestion = requireSafeSuggestion(
                caseId, request.suggestionId());
        if (!"SUGGESTED".equals(resolutionCase.status())) {
            throw new AuraStateException("Only suggested resolution cases can be approved.");
        }
        if (!repository.updateCaseStatus(
                caseId, request.version(), "SUGGESTED", "APPROVED",
                userId, request.reason().trim())) {
            throw conflict();
        }
        repository.insertAction(
                caseId, suggestion.id(), userId, "APPROVED", request.reason());
        notificationService.notifyStudent(
                resolutionCase.studentUserId(),
                "Timetable resolution approved",
                "Your timetable resolution has been approved and is ready to apply.",
                caseId);
        return requireCase(caseId);
    }

    @Transactional
    public ResolutionCaseResponse reject(
            UUID userId,
            UUID caseId,
            ResolutionDecisionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ResolutionCaseResponse resolutionCase = requireAdminCase(caseId, universityId);
        if (!Set.of("OPEN", "SUGGESTED", "APPROVED")
                .contains(resolutionCase.status())) {
            throw new AuraStateException("This resolution case can no longer be rejected.");
        }
        if (!repository.updateCaseStatus(
                caseId, request.version(), resolutionCase.status(), "REJECTED",
                userId, request.reason().trim())) {
            throw conflict();
        }
        repository.insertAction(
                caseId, request.suggestionId(), userId, "REJECTED", request.reason());
        notificationService.notifyStudent(
                resolutionCase.studentUserId(),
                "Timetable resolution reviewed",
                "Your timetable resolution request was not approved.",
                caseId);
        return requireCase(caseId);
    }

    @Transactional
    public ResolutionCaseResponse apply(
            UUID userId,
            UUID caseId,
            ResolutionDecisionRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ResolutionCaseResponse resolutionCase = requireAdminCase(caseId, universityId);
        if (!"APPROVED".equals(resolutionCase.status())) {
            throw new AuraStateException("Approve this resolution before applying it.");
        }
        RegistrationScope registration = repository
                .findRegistrationScope(resolutionCase.registrationId())
                .orElseThrow(() -> notFound("AURA registration was not found."));
        SuggestionTarget suggestion = requireSafeSuggestion(
                caseId, request.suggestionId());
        if (suggestion.offeringId() != null) {
            OfferingCandidate currentCandidate = repository
                    .parallelOfferingCandidates(registration).stream()
                    .filter(candidate -> candidate.offeringId().equals(
                            suggestion.offeringId()))
                    .findFirst()
                    .orElseThrow(() -> unavailableAlternative());
            requireStillSafe(
                    currentCandidate.clashFree(),
                    currentCandidate.remainingCapacity());
            repository.applyOfferingTransfer(
                    caseId, resolutionCase.registrationId(), suggestion,
                    userId, request.reason().trim());
        } else {
            GroupCandidate groupCandidate = repository
                    .alternateGroupCandidates(registration).stream()
                    .filter(candidate -> candidate.groupId().equals(
                            suggestion.groupId()))
                    .findFirst()
                    .orElseThrow(() -> unavailableAlternative());
            requireStillSafe(
                    groupCandidate.clashFree(),
                    groupCandidate.remainingCapacity());
            repository.applyGroupTransfer(
                    caseId, resolutionCase.registrationId(), suggestion,
                    groupCandidate.groupType(), userId, request.reason().trim());
        }
        notificationService.notifyStudent(
                resolutionCase.studentUserId(),
                "Personal timetable updated",
                "Your approved timetable resolution has been applied.",
                caseId);
        return requireCase(caseId);
    }

    private ResolutionCaseResponse requireAdminCase(
            UUID caseId,
            UUID universityId) {
        ResolutionCaseResponse resolutionCase = requireCase(caseId);
        RegistrationScope registration = repository
                .findRegistrationScope(resolutionCase.registrationId())
                .orElseThrow(() -> notFound("AURA resolution case was not found."));
        if (!registration.universityId().equals(universityId)) {
            throw notFound("AURA resolution case was not found.");
        }
        return resolutionCase;
    }

    private ResolutionCaseResponse requireCase(UUID caseId) {
        return repository.findCase(caseId)
                .orElseThrow(() -> notFound("AURA resolution case was not found."));
    }

    private SuggestionTarget requireSafeSuggestion(UUID caseId, UUID suggestionId) {
        if (suggestionId == null) {
            throw new AuraStateException("Choose a safe resolution suggestion.");
        }
        SuggestionTarget suggestion = repository.findSuggestion(caseId, suggestionId)
                .orElseThrow(() -> notFound("AURA resolution suggestion was not found."));
        if (!suggestion.safe()
                || (suggestion.offeringId() == null && suggestion.groupId() == null)) {
            throw new AuraStateException("Only a safe timetable alternative can be approved.");
        }
        return suggestion;
    }

    private void requireStillSafe(boolean clashFree, int remainingCapacity) {
        if (!clashFree || remainingCapacity <= 0) {
            throw new AuraStateException(
                    "This alternative is no longer safe. Analyze the case again.");
        }
    }

    private AuraStateException unavailableAlternative() {
        return new AuraStateException(
                "This alternative is no longer available. Analyze the case again.");
    }

    private void requireTerm(UUID termId, UUID universityId) {
        if (!auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, termId, universityId)) {
            throw notFound("AURA term was not found.");
        }
    }

    private String normalizeCaseType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!CASE_TYPES.contains(normalized)) {
            throw new AuraStateException("Choose a supported timetable resolution type.");
        }
        return normalized;
    }

    private AuraStateException conflict() {
        return new AuraStateException(
                "This resolution case changed while you were reviewing it. Refresh and try again.");
    }

    private ResourceNotFoundException notFound(String message) {
        String suffix = " was not found.";
        return new ResourceNotFoundException(message.endsWith(suffix)
                ? message.substring(0, message.length() - suffix.length())
                : message);
    }
}
