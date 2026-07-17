package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraRegistrationDtos;
import com.campusone.aura.dto.AuraResolutionDtos;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionCaseResponse;
import com.campusone.aura.dto.AuraScenarioDtos;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableResponse;
import com.campusone.aura.dto.AuraRegistrationDtos.StudentRegistrationResponse;
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
import com.campusone.aura.service.AuraRegistrationService;
import com.campusone.aura.service.AuraExportService;
import com.campusone.aura.service.AuraResolutionService;
import com.campusone.aura.service.AuraScenarioService;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final AuraRegistrationService registrationService;
    private final AuraExportService exportService;
    private final AuraResolutionService resolutionService;
    private final AuraScenarioService scenarioService;

    public AuraAdminController(
            AuraService auraService,
            AuraRegistrationService registrationService,
            AuraExportService exportService,
            AuraResolutionService resolutionService,
            AuraScenarioService scenarioService) {
        this.auraService = auraService;
        this.registrationService = registrationService;
        this.exportService = exportService;
        this.resolutionService = resolutionService;
        this.scenarioService = scenarioService;
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

    @GetMapping("/setup-references")
    @Operation(summary = "List university-scoped setup reference data")
    public ResponseEntity<AuraDtos.SetupReferencesResponse> setupReferences(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(auraService.setupReferences(principal.getUserId()));
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

    @PutMapping("/rooms/{roomId}/facilities")
    @Operation(summary = "Replace room facilities")
    public ResponseEntity<AuraDtos.FacilitySetResponse> replaceRoomFacilities(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID roomId,
            @Valid @RequestBody AuraDtos.ReplaceFacilitiesRequest request) {
        return ResponseEntity.ok(auraService.replaceRoomFacilities(
                principal.getUserId(),
                roomId,
                request));
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

    @PostMapping("/section-availability")
    @Operation(summary = "Create or update section availability")
    public ResponseEntity<AvailabilityResponse> upsertSectionAvailability(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraDtos.CreateSectionAvailabilityRequest
                    request) {
        return ResponseEntity.ok(auraService.upsertSectionAvailability(
                principal.getUserId(),
                request));
    }

    @GetMapping("/sections/{sectionId}/availability")
    @Operation(summary = "List section availability")
    public ResponseEntity<List<AvailabilityResponse>> listSectionAvailability(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sectionId) {
        return ResponseEntity.ok(auraService.listSectionAvailability(
                principal.getUserId(),
                sectionId));
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

    @PutMapping("/meeting-requirements/{requirementId}/facilities")
    @Operation(summary = "Replace meeting requirement facilities")
    public ResponseEntity<AuraDtos.FacilitySetResponse>
            replaceMeetingRequirementFacilities(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID requirementId,
                    @Valid @RequestBody AuraDtos.ReplaceFacilitiesRequest request) {
        return ResponseEntity.ok(auraService.replaceRequirementFacilities(
                principal.getUserId(),
                requirementId,
                request));
    }

    @PostMapping("/calendar-exceptions")
    @Operation(summary = "Create a term calendar exception")
    public ResponseEntity<AuraDtos.CalendarExceptionResponse>
            createCalendarException(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @Valid @RequestBody AuraDtos.CreateCalendarExceptionRequest request) {
        AuraDtos.CalendarExceptionResponse created =
                auraService.createCalendarException(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/aura/calendar-exceptions/" + created.id()))
                .body(created);
    }

    @GetMapping("/terms/{termId}/calendar-exceptions")
    @Operation(summary = "List term calendar exceptions")
    public ResponseEntity<List<AuraDtos.CalendarExceptionResponse>>
            listCalendarExceptions(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID termId) {
        return ResponseEntity.ok(auraService.listCalendarExceptions(
                principal.getUserId(),
                termId));
    }

    @PatchMapping("/calendar-exceptions/{exceptionId}")
    @Operation(summary = "Update a term calendar exception")
    public ResponseEntity<AuraDtos.CalendarExceptionResponse>
            updateCalendarException(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID exceptionId,
                    @Valid @RequestBody AuraDtos.UpdateCalendarExceptionRequest request) {
        return ResponseEntity.ok(auraService.updateCalendarException(
                principal.getUserId(),
                exceptionId,
                request));
    }

    @DeleteMapping("/calendar-exceptions/{exceptionId}")
    @Operation(summary = "Deactivate a term calendar exception")
    public ResponseEntity<Void> deactivateCalendarException(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID exceptionId,
            @RequestParam @Min(0) long version) {
        auraService.deactivateCalendarException(
                principal.getUserId(),
                exceptionId,
                version);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/versions/{versionId}/clone")
    @Operation(summary = "Clone a timetable version into a new editable draft")
    public ResponseEntity<TimetableVersionResponse> cloneVersion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId,
            @Valid @RequestBody AuraDtos.CloneVersionRequest request) {
        return ResponseEntity.ok(auraService.cloneVersion(
                principal.getUserId(), versionId, request));
    }

    @GetMapping("/versions/{versionId}/compare/{otherVersionId}")
    @Operation(summary = "Compare two timetable versions from the same term")
    public ResponseEntity<AuraDtos.VersionComparisonResponse> compareVersions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId,
            @PathVariable UUID otherVersionId) {
        return ResponseEntity.ok(auraService.compareVersions(
                principal.getUserId(), versionId, otherVersionId));
    }

    @PostMapping("/versions/{versionId}/archive")
    @Operation(summary = "Archive an editable timetable draft")
    public ResponseEntity<TimetableVersionResponse> archiveVersion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId) {
        return ResponseEntity.ok(auraService.archiveVersion(
                principal.getUserId(), versionId));
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

    @GetMapping("/versions/{versionId}/export")
    @Operation(summary = "Export a timetable version")
    public ResponseEntity<byte[]> exportVersion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID versionId,
            @RequestParam(defaultValue = "CSV") String format) {
        AuraExportService.ExportPayload export = exportService.export(
                principal.getUserId(), versionId, format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(export.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(export.filename())
                .build());
        return ResponseEntity.ok().headers(headers).body(export.bytes());
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

    @PostMapping("/sessions/{sessionId}/swap-preview")
    @Operation(summary = "Preview a safe swap between two draft sessions")
    public ResponseEntity<AuraDtos.ManualMovePreviewResponse> previewSwap(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AuraDtos.SessionSwapPreviewRequest request) {
        return ResponseEntity.ok(auraService.previewSwap(
                principal.getUserId(), sessionId, request));
    }

    @PatchMapping("/sessions/{sessionId}/swap")
    @Operation(summary = "Apply a clash-safe swap between two draft sessions")
    public ResponseEntity<List<SessionResponse>> applySwap(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AuraDtos.SessionSwapRequest request) {
        return ResponseEntity.ok(auraService.applySwap(
                principal.getUserId(), sessionId, request));
    }

    @PatchMapping("/sessions/{sessionId}/pin")
    @Operation(summary = "Pin or unpin a session in an editable draft")
    public ResponseEntity<SessionResponse> setSessionPinned(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AuraDtos.SessionPinRequest request) {
        return ResponseEntity.ok(auraService.setSessionPinned(
                principal.getUserId(), sessionId, request));
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

    @PostMapping("/registrations")
    @Operation(summary = "Create an individual student course registration")
    public ResponseEntity<StudentRegistrationResponse> createRegistration(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraRegistrationDtos.CreateRegistrationRequest request) {
        StudentRegistrationResponse response = registrationService.createRegistration(
                principal.getUserId(), request);
        return ResponseEntity.created(
                        URI.create("/api/v1/admin/aura/registrations/" + response.id()))
                .body(response);
    }

    @GetMapping("/terms/{termId}/registrations")
    @Operation(summary = "List student registrations for a term")
    public ResponseEntity<List<StudentRegistrationResponse>> listRegistrations(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @RequestParam(required = false) UUID studentUserId) {
        return ResponseEntity.ok(registrationService.listRegistrations(
                principal.getUserId(), termId, studentUserId));
    }

    @PatchMapping("/registrations/{registrationId}")
    @Operation(summary = "Update a student registration with optimistic locking")
    public ResponseEntity<StudentRegistrationResponse> updateRegistration(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID registrationId,
            @Valid @RequestBody AuraRegistrationDtos.UpdateRegistrationRequest request) {
        return ResponseEntity.ok(registrationService.updateRegistration(
                principal.getUserId(), registrationId, request));
    }

    @GetMapping("/terms/{termId}/students/{studentUserId}/timetable")
    @Operation(summary = "Inspect a student's personal timetable")
    public ResponseEntity<PersonalTimetableResponse> studentTimetable(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @PathVariable UUID studentUserId) {
        return ResponseEntity.ok(registrationService.adminPersonalTimetable(
                principal.getUserId(), termId, studentUserId));
    }

    @GetMapping("/terms/{termId}/resolution-cases")
    @Operation(summary = "List timetable resolution cases for a term")
    public ResponseEntity<List<ResolutionCaseResponse>> listResolutionCases(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(resolutionService.listAdminCases(
                principal.getUserId(), termId));
    }

    @PostMapping("/resolution-cases/{caseId}/analyze")
    @Operation(summary = "Generate ranked student-only resolution suggestions")
    public ResponseEntity<ResolutionCaseResponse> analyzeResolutionCase(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID caseId) {
        return ResponseEntity.ok(resolutionService.analyze(
                principal.getUserId(), caseId));
    }

    @PostMapping("/resolution-cases/{caseId}/approve")
    @Operation(summary = "Approve a safe timetable resolution suggestion")
    public ResponseEntity<ResolutionCaseResponse> approveResolutionCase(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID caseId,
            @Valid @RequestBody AuraResolutionDtos.ResolutionDecisionRequest request) {
        return ResponseEntity.ok(resolutionService.approve(
                principal.getUserId(), caseId, request));
    }

    @PostMapping("/resolution-cases/{caseId}/reject")
    @Operation(summary = "Reject a timetable resolution case")
    public ResponseEntity<ResolutionCaseResponse> rejectResolutionCase(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID caseId,
            @Valid @RequestBody AuraResolutionDtos.ResolutionDecisionRequest request) {
        return ResponseEntity.ok(resolutionService.reject(
                principal.getUserId(), caseId, request));
    }

    @PostMapping("/resolution-cases/{caseId}/apply")
    @Operation(summary = "Apply an approved student-only timetable resolution")
    public ResponseEntity<ResolutionCaseResponse> applyResolutionCase(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID caseId,
            @Valid @RequestBody AuraResolutionDtos.ResolutionDecisionRequest request) {
        return ResponseEntity.ok(resolutionService.apply(
                principal.getUserId(), caseId, request));
    }

    @PostMapping("/terms/{termId}/what-if")
    @Operation(summary = "Run a non-destructive timetable what-if analysis")
    public ResponseEntity<AuraScenarioDtos.WhatIfResponse> runWhatIf(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId,
            @Valid @RequestBody AuraScenarioDtos.WhatIfRequest request) {
        return ResponseEntity.ok(scenarioService.runWhatIf(
                principal.getUserId(), termId, request));
    }

    @GetMapping("/terms/{termId}/what-if")
    @Operation(summary = "List timetable what-if analyses")
    public ResponseEntity<List<AuraScenarioDtos.WhatIfResponse>> listWhatIf(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID termId) {
        return ResponseEntity.ok(scenarioService.listWhatIf(
                principal.getUserId(), termId));
    }

    @PostMapping("/terms/{termId}/emergency-repairs")
    @Operation(summary = "Create a minimally scoped emergency repair draft")
    public ResponseEntity<AuraScenarioDtos.EmergencyRepairResponse>
            createEmergencyRepair(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID termId,
                    @Valid @RequestBody AuraScenarioDtos.EmergencyRepairRequest request) {
        return ResponseEntity.accepted().body(scenarioService.createEmergencyDraft(
                principal.getUserId(), termId, request));
    }

    @GetMapping("/terms/{termId}/emergency-repairs")
    @Operation(summary = "List emergency repair requests")
    public ResponseEntity<List<AuraScenarioDtos.EmergencyRepairResponse>>
            listEmergencyRepairs(
                    @AuthenticationPrincipal CampusOneUserPrincipal principal,
                    @PathVariable UUID termId) {
        return ResponseEntity.ok(scenarioService.listEmergencies(
                principal.getUserId(), termId));
    }
}
