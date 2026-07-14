package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraDtos.AvailabilityResponse;
import com.campusone.aura.dto.AuraDtos.BatchResponse;
import com.campusone.aura.dto.AuraDtos.ClashResponse;
import com.campusone.aura.dto.AuraDtos.GenerationRunResponse;
import com.campusone.aura.dto.AuraDtos.InstructorResponse;
import com.campusone.aura.dto.AuraDtos.MeetingRequirementResponse;
import com.campusone.aura.dto.AuraDtos.OfferingResponse;
import com.campusone.aura.dto.AuraDtos.PageResponse;
import com.campusone.aura.dto.AuraDtos.ProgramResponse;
import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.dto.AuraDtos.RoomResponse;
import com.campusone.aura.dto.AuraDtos.SectionResponse;
import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.dto.AuraDtos.TimeslotResponse;
import com.campusone.aura.service.AuraService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
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
@Tag(name = "AURA")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraAdminController {

    private final AuraService auraService;

    public AuraAdminController(AuraService auraService) {
        this.auraService = auraService;
    }

    @PostMapping("/terms")
    @Operation(summary = "Create an AURA academic term")
    public ResponseEntity<TermResponse> createTerm(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateTermRequest request) {
        TermResponse response = auraService.createTerm(
                principal.getUserId(),
                request);
        return ResponseEntity.created(
                        URI.create("/api/v1/admin/aura/terms/" + response.id()))
                .body(response);
    }

    @GetMapping("/terms")
    @Operation(summary = "List AURA academic terms")
    public ResponseEntity<PageResponse<TermResponse>> listTerms(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(auraService.listTerms(
                principal.getUserId(),
                page,
                size));
    }

    @PostMapping("/programs")
    @Operation(summary = "Create an AURA program")
    public ResponseEntity<ProgramResponse> createProgram(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateProgramRequest request) {
        return ResponseEntity.ok(auraService.createProgram(
                principal.getUserId(),
                request));
    }

    @GetMapping("/programs")
    @Operation(summary = "List AURA programs")
    public ResponseEntity<List<ProgramResponse>> listPrograms(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID universityId) {
        return ResponseEntity.ok(auraService.listPrograms(
                principal.getUserId(),
                universityId));
    }

    @PostMapping("/batches")
    @Operation(summary = "Create an AURA batch")
    public ResponseEntity<BatchResponse> createBatch(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateBatchRequest request) {
        return ResponseEntity.ok(auraService.createBatch(
                principal.getUserId(),
                request));
    }

    @GetMapping("/batches")
    @Operation(summary = "List AURA batches")
    public ResponseEntity<List<BatchResponse>> listBatches(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID programId) {
        return ResponseEntity.ok(auraService.listBatches(
                principal.getUserId(),
                programId));
    }

    @PostMapping("/sections")
    @Operation(summary = "Create an AURA section")
    public ResponseEntity<SectionResponse> createSection(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateSectionRequest request) {
        return ResponseEntity.ok(auraService.createSection(
                principal.getUserId(),
                request));
    }

    @GetMapping("/sections")
    @Operation(summary = "List AURA sections")
    public ResponseEntity<List<SectionResponse>> listSections(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID batchId) {
        return ResponseEntity.ok(auraService.listSections(
                principal.getUserId(),
                batchId));
    }

    @PostMapping("/instructors")
    @Operation(summary = "Create an AURA instructor")
    public ResponseEntity<InstructorResponse> createInstructor(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateInstructorRequest request) {
        return ResponseEntity.ok(auraService.createInstructor(
                principal.getUserId(),
                request));
    }

    @GetMapping("/instructors")
    @Operation(summary = "List AURA instructors")
    public ResponseEntity<List<InstructorResponse>> listInstructors(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID universityId) {
        return ResponseEntity.ok(auraService.listInstructors(
                principal.getUserId(),
                universityId));
    }

    @PostMapping("/rooms")
    @Operation(summary = "Create an AURA room")
    public ResponseEntity<RoomResponse> createRoom(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateRoomRequest request) {
        return ResponseEntity.ok(auraService.createRoom(
                principal.getUserId(),
                request));
    }

    @GetMapping("/rooms")
    @Operation(summary = "List AURA rooms")
    public ResponseEntity<List<RoomResponse>> listRooms(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID universityId) {
        return ResponseEntity.ok(auraService.listRooms(
                principal.getUserId(),
                universityId));
    }

    @PostMapping("/timeslots")
    @Operation(summary = "Create an AURA timeslot")
    public ResponseEntity<TimeslotResponse> createTimeslot(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateTimeslotRequest request) {
        return ResponseEntity.ok(auraService.createTimeslot(
                principal.getUserId(),
                request));
    }

    @GetMapping("/timeslots")
    @Operation(summary = "List AURA timeslots")
    public ResponseEntity<List<TimeslotResponse>> listTimeslots(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) UUID universityId) {
        return ResponseEntity.ok(auraService.listTimeslots(
                principal.getUserId(),
                universityId));
    }

    @PostMapping("/instructor-availability")
    @Operation(summary = "Create or update instructor availability")
    public ResponseEntity<AvailabilityResponse> upsertInstructorAvailability(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateInstructorAvailabilityRequest
                    request) {
        return ResponseEntity.ok(auraService.upsertInstructorAvailability(
                principal.getUserId(),
                request));
    }

    @GetMapping("/instructors/{instructorId}/availability")
    @Operation(summary = "List instructor availability")
    public ResponseEntity<List<AvailabilityResponse>>
            listInstructorAvailability(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID instructorId) {
        return ResponseEntity.ok(auraService.listInstructorAvailability(
                principal.getUserId(),
                instructorId));
    }

    @PostMapping("/room-availability")
    @Operation(summary = "Create or update room availability")
    public ResponseEntity<AvailabilityResponse> upsertRoomAvailability(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateRoomAvailabilityRequest
                    request) {
        return ResponseEntity.ok(auraService.upsertRoomAvailability(
                principal.getUserId(),
                request));
    }

    @GetMapping("/rooms/{roomId}/availability")
    @Operation(summary = "List room availability")
    public ResponseEntity<List<AvailabilityResponse>> listRoomAvailability(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID roomId) {
        return ResponseEntity.ok(auraService.listRoomAvailability(
                principal.getUserId(),
                roomId));
    }

    @PostMapping("/offerings")
    @Operation(summary = "Create an AURA course offering")
    public ResponseEntity<OfferingResponse> createOffering(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateOfferingRequest request) {
        return ResponseEntity.ok(auraService.createOffering(
                principal.getUserId(),
                request));
    }

    @GetMapping("/terms/{termId}/offerings")
    @Operation(summary = "List AURA course offerings for a term")
    public ResponseEntity<List<OfferingResponse>> listOfferings(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(auraService.listOfferings(
                principal.getUserId(),
                termId));
    }

    @PostMapping("/meeting-requirements")
    @Operation(summary = "Create an AURA meeting requirement")
    public ResponseEntity<MeetingRequirementResponse> createMeetingRequirement(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateMeetingRequirementRequest request) {
        return ResponseEntity.ok(auraService.createMeetingRequirement(
                principal.getUserId(),
                request));
    }

    @GetMapping("/offerings/{offeringId}/meeting-requirements")
    @Operation(summary = "List meeting requirements for an offering")
    public ResponseEntity<List<MeetingRequirementResponse>>
            listMeetingRequirements(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID offeringId) {
        return ResponseEntity.ok(auraService.listMeetingRequirements(
                principal.getUserId(),
                offeringId));
    }

    @GetMapping("/terms/{termId}/readiness")
    @Operation(summary = "Validate whether a term is ready for generation")
    public ResponseEntity<ReadinessResponse> readiness(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(auraService.readiness(
                principal.getUserId(),
                termId));
    }

    @PostMapping("/terms/{termId}/runs")
    @Operation(summary = "Start an asynchronous AURA generation run")
    public ResponseEntity<GenerationRunResponse> startGeneration(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @Valid @RequestBody AuraDtos.GenerateTimetableRequest request) {
        return ResponseEntity.accepted().body(auraService.startGeneration(
                principal.getUserId(),
                termId,
                request));
    }

    @GetMapping("/runs/{runId}")
    @Operation(summary = "Get an AURA generation run")
    public ResponseEntity<GenerationRunResponse> getRun(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID runId) {
        return ResponseEntity.ok(auraService.getRun(
                principal.getUserId(),
                runId));
    }

    @PostMapping("/runs/{runId}/cancel")
    @Operation(summary = "Cancel an active AURA generation run")
    public ResponseEntity<GenerationRunResponse> cancelRun(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID runId) {
        return ResponseEntity.ok(auraService.cancelRun(
                principal.getUserId(),
                runId));
    }

    @GetMapping("/terms/{termId}/versions")
    @Operation(summary = "List timetable versions for a term")
    public ResponseEntity<List<TimetableVersionResponse>> listVersions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(auraService.listVersions(
                principal.getUserId(),
                termId));
    }

    @GetMapping("/versions/{versionId}")
    @Operation(summary = "Get a timetable version")
    public ResponseEntity<TimetableVersionResponse> getVersion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId) {
        return ResponseEntity.ok(auraService.getVersion(
                principal.getUserId(),
                versionId));
    }

    @PostMapping("/versions/{versionId}/publish")
    @Operation(summary = "Publish a timetable version")
    public ResponseEntity<TimetableVersionResponse> publishVersion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId) {
        return ResponseEntity.ok(auraService.publishVersion(
                principal.getUserId(),
                versionId));
    }

    @GetMapping("/versions/{versionId}/sessions")
    @Operation(summary = "List scheduled sessions for a timetable version")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId) {
        return ResponseEntity.ok(auraService.listSessions(
                principal.getUserId(),
                versionId));
    }

    @GetMapping("/versions/{versionId}/clashes")
    @Operation(summary = "List unresolved clashes for a timetable version")
    public ResponseEntity<List<ClashResponse>> listClashes(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId) {
        return ResponseEntity.ok(auraService.listClashes(
                principal.getUserId(),
                versionId));
    }

    @PostMapping("/sessions/{sessionId}/move-preview")
    @Operation(summary = "Preview a manual session move")
    public ResponseEntity<AuraDtos.ManualMovePreviewResponse> previewMove(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AuraDtos.ManualMovePreviewRequest request) {
        return ResponseEntity.ok(auraService.previewMove(
                principal.getUserId(),
                sessionId,
                request));
    }

    @PatchMapping("/sessions/{sessionId}/move")
    @Operation(summary = "Apply a clash-safe manual session move")
    public ResponseEntity<SessionResponse> applyMove(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AuraDtos.ManualMoveRequest request) {
        return ResponseEntity.ok(auraService.applyMove(
                principal.getUserId(),
                sessionId,
                request));
    }

    @GetMapping("/terms/{termId}/metrics")
    @Operation(summary = "Get AURA timetable metrics")
    public ResponseEntity<AuraDtos.AuraMetricsResponse> metrics(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(auraService.metrics(
                principal.getUserId(),
                termId));
    }
}
