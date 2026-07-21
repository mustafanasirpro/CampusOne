package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.service.AuraOperationsService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/aura")
@Validated
@Tag(name = "AURA Operations")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraOperationsController {

    private final AuraOperationsService service;

    public AuraOperationsController(AuraOperationsService service) {
        this.service = service;
    }

    @PatchMapping("/{resourceType}/{id}/active-state")
    @Operation(summary = "Activate or deactivate an AURA setup resource")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> setActiveState(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable String resourceType,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.ActiveStateRequest request) {
        return ResponseEntity.ok(service.setActiveState(
                principal.getUserId(), resourceType, id, request));
    }

    @PatchMapping("/terms/{id}")
    @Operation(summary = "Update or archive an academic term with optimistic locking")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateTerm(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateTermRequest request) {
        return ResponseEntity.ok(service.updateTerm(principal.getUserId(), id, request));
    }

    @PatchMapping("/programs/{id}")
    @Operation(summary = "Update or deactivate an academic program")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateProgram(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateProgramRequest request) {
        return ResponseEntity.ok(service.updateProgram(principal.getUserId(), id, request));
    }

    @PatchMapping("/batches/{id}")
    @Operation(summary = "Update or deactivate a student batch")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateBatch(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateBatchRequest request) {
        return ResponseEntity.ok(service.updateBatch(principal.getUserId(), id, request));
    }

    @PatchMapping("/sections/{id}")
    @Operation(summary = "Update or deactivate a timetable section")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateSection(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateSectionRequest request) {
        return ResponseEntity.ok(service.updateSection(principal.getUserId(), id, request));
    }

    @PatchMapping("/instructors/{id}")
    @Operation(summary = "Update or deactivate an instructor")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateInstructor(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateInstructorRequest request) {
        return ResponseEntity.ok(service.updateInstructor(principal.getUserId(), id, request));
    }

    @PatchMapping("/rooms/{id}")
    @Operation(summary = "Update or deactivate a room and its facilities")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateRoom(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateRoomRequest request) {
        return ResponseEntity.ok(service.updateRoom(principal.getUserId(), id, request));
    }

    @PatchMapping("/timeslots/{id}")
    @Operation(summary = "Update or deactivate a timeslot")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateTimeslot(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateTimeslotRequest request) {
        return ResponseEntity.ok(service.updateTimeslot(principal.getUserId(), id, request));
    }

    @PatchMapping("/offerings/{id}")
    @Operation(summary = "Update or deactivate a course offering")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateOffering(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateOfferingRequest request) {
        return ResponseEntity.ok(service.updateOffering(principal.getUserId(), id, request));
    }

    @PatchMapping("/meeting-requirements/{id}")
    @Operation(summary = "Update or deactivate a complete meeting requirement")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateRequirement(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateMeetingRequirementRequest request) {
        return ResponseEntity.ok(service.updateRequirement(principal.getUserId(), id, request));
    }

    @PostMapping("/buildings")
    @Operation(summary = "Create a university building")
    public ResponseEntity<AuraOperationsDtos.BuildingResponse> createBuilding(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraOperationsDtos.CreateBuildingRequest request) {
        var response = service.createBuilding(principal.getUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/admin/aura/buildings/" + response.id()))
                .body(response);
    }

    @GetMapping("/buildings")
    @Operation(summary = "List university buildings")
    public ResponseEntity<List<AuraOperationsDtos.BuildingResponse>> listBuildings(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(service.listBuildings(principal.getUserId()));
    }

    @PatchMapping("/buildings/{id}")
    @Operation(summary = "Update or deactivate a university building")
    public ResponseEntity<AuraOperationsDtos.MutationResponse> updateBuilding(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpdateBuildingRequest request) {
        return ResponseEntity.ok(service.updateBuilding(principal.getUserId(), id, request));
    }

    @PostMapping("/teaching-groups")
    @Operation(summary = "Create a lecture, laboratory, or tutorial group")
    public ResponseEntity<AuraOperationsDtos.TeachingGroupResponse> createTeachingGroup(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        var response = service.createTeachingGroup(principal.getUserId(), request);
        return ResponseEntity.created(
                URI.create("/api/v1/admin/aura/teaching-groups/" + response.id())).body(response);
    }

    @GetMapping("/terms/{termId}/teaching-groups")
    @Operation(summary = "List teaching groups for a term")
    public ResponseEntity<List<AuraOperationsDtos.TeachingGroupResponse>> listTeachingGroups(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(service.listTeachingGroups(principal.getUserId(), termId));
    }

    @PatchMapping("/teaching-groups/{id}")
    @Operation(summary = "Update or deactivate a teaching group")
    public ResponseEntity<AuraOperationsDtos.TeachingGroupResponse> updateTeachingGroup(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpsertTeachingGroupRequest request) {
        return ResponseEntity.ok(service.updateTeachingGroup(principal.getUserId(), id, request));
    }

    @PostMapping("/student-availability")
    @Operation(summary = "Create or update student availability")
    public ResponseEntity<AuraOperationsDtos.StudentAvailabilityResponse>
            upsertStudentAvailability(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @Valid @RequestBody
                    AuraOperationsDtos.UpsertStudentAvailabilityRequest request) {
        return ResponseEntity.ok(service.upsertStudentAvailability(principal.getUserId(), request));
    }

    @GetMapping("/terms/{termId}/student-availability")
    @Operation(summary = "List student availability for a term")
    public ResponseEntity<List<AuraOperationsDtos.StudentAvailabilityResponse>>
            listStudentAvailability(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID termId,
                    @RequestParam(required = false) UUID studentUserId) {
        return ResponseEntity.ok(service.listStudentAvailability(
                principal.getUserId(), termId, studentUserId));
    }

    @PostMapping("/offering-conflicts")
    @Operation(summary = "Create a hard or medium course-offering conflict")
    public ResponseEntity<AuraOperationsDtos.OfferingConflictResponse> createConflict(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        var response = service.createOfferingConflict(principal.getUserId(), request);
        return ResponseEntity.created(
                URI.create("/api/v1/admin/aura/offering-conflicts/" + response.id())).body(response);
    }

    @GetMapping("/terms/{termId}/offering-conflicts")
    @Operation(summary = "List course-offering conflicts")
    public ResponseEntity<List<AuraOperationsDtos.OfferingConflictResponse>> listConflicts(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(service.listOfferingConflicts(principal.getUserId(), termId));
    }

    @PatchMapping("/offering-conflicts/{id}")
    @Operation(summary = "Update or deactivate a course-offering conflict")
    public ResponseEntity<AuraOperationsDtos.OfferingConflictResponse> updateConflict(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpsertOfferingConflictRequest request) {
        return ResponseEntity.ok(service.updateOfferingConflict(principal.getUserId(), id, request));
    }

    @PostMapping("/travel-rules")
    @Operation(summary = "Create a building travel-time rule")
    public ResponseEntity<AuraOperationsDtos.TravelRuleResponse> createTravelRule(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraOperationsDtos.UpsertTravelRuleRequest request) {
        var response = service.createTravelRule(principal.getUserId(), request);
        return ResponseEntity.created(
                URI.create("/api/v1/admin/aura/travel-rules/" + response.id())).body(response);
    }

    @GetMapping("/travel-rules")
    @Operation(summary = "List university building travel-time rules")
    public ResponseEntity<List<AuraOperationsDtos.TravelRuleResponse>> listTravelRules(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(service.listTravelRules(principal.getUserId()));
    }

    @PatchMapping("/travel-rules/{id}")
    @Operation(summary = "Update or deactivate a building travel-time rule")
    public ResponseEntity<AuraOperationsDtos.TravelRuleResponse> updateTravelRule(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AuraOperationsDtos.UpsertTravelRuleRequest request) {
        return ResponseEntity.ok(service.updateTravelRule(principal.getUserId(), id, request));
    }

    @GetMapping("/audit")
    @Operation(summary = "List university-scoped AURA audit events")
    public ResponseEntity<List<AuraOperationsDtos.AuditEventResponse>> listAudit(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(service.listAudit(principal.getUserId(), termId,
                action, targetType, from, to, page, size));
    }

    @GetMapping("/versions/{versionId}/timetable-view")
    @Operation(summary = "View a timetable by instructor, section, room, course, offering, program, department, day, or week")
    public ResponseEntity<AuraOperationsDtos.ScopedTimetableResponse> timetableView(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId,
            @RequestParam(defaultValue = "WEEK") String scope,
            @RequestParam(required = false) UUID scopeId,
            @RequestParam(required = false) @Min(1) @Max(7) Integer dayOfWeek) {
        return ResponseEntity.ok(service.scopedTimetable(
                principal.getUserId(), versionId, scope, scopeId, dayOfWeek));
    }

    @GetMapping("/terms/{termId}/analytics")
    @Operation(summary = "Get persisted AURA utilization, load, clash, and repair analytics")
    public ResponseEntity<AuraOperationsDtos.AnalyticsResponse> analytics(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @RequestParam UUID versionId) {
        return ResponseEntity.ok(service.analytics(principal.getUserId(), termId, versionId));
    }
}
