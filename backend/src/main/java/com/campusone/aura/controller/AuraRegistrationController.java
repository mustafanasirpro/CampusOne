package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableResponse;
import com.campusone.aura.dto.AuraRegistrationDtos.StudentRegistrationResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraResolutionDtos;
import com.campusone.aura.dto.AuraResolutionDtos.ResolutionCaseResponse;
import com.campusone.aura.service.AuraRegistrationService;
import com.campusone.aura.service.AuraResolutionService;
import com.campusone.aura.service.AuraAuthorizationService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aura")
@Tag(name = "AURA personal timetable")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraRegistrationController {

    private final AuraRegistrationService registrationService;
    private final AuraResolutionService resolutionService;
    private final AuraAuthorizationService authorizationService;

    public AuraRegistrationController(
            AuraRegistrationService registrationService,
            AuraResolutionService resolutionService,
            AuraAuthorizationService authorizationService) {
        this.registrationService = registrationService;
        this.resolutionService = resolutionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/capabilities")
    @Operation(summary = "Get the signed-in user's AURA capabilities")
    public ResponseEntity<com.campusone.aura.dto.AuraDtos.CapabilitiesResponse>
            capabilities(@AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID universityId = authorizationService.requireUniversity(
                principal.getUserId());
        return ResponseEntity.ok(
                new com.campusone.aura.dto.AuraDtos.CapabilitiesResponse(
                        universityId,
                        authorizationService.canManage(principal.getUserId())));
    }

    @GetMapping("/me/registrations")
    @Operation(summary = "List the signed-in student's AURA registrations")
    public ResponseEntity<List<StudentRegistrationResponse>> myRegistrations(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(registrationService.listMyRegistrations(
                principal.getUserId(), termId));
    }

    @GetMapping("/terms")
    @Operation(summary = "List AURA terms with a published timetable")
    public ResponseEntity<List<TermResponse>> availableTerms(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(registrationService.listAvailableTerms(
                principal.getUserId()));
    }

    @GetMapping("/me/timetable")
    @Operation(summary = "Get the signed-in student's published timetable")
    public ResponseEntity<PersonalTimetableResponse> myTimetable(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(registrationService.personalTimetable(
                principal.getUserId(), termId));
    }

    @GetMapping(value = "/me/timetable.ics", produces = "text/calendar")
    @Operation(summary = "Download the signed-in student's personal timetable calendar")
    public ResponseEntity<byte[]> myTimetableCalendar(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam UUID termId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=campusone-aura-timetable.ics")
                .body(registrationService.personalTimetableCalendar(
                        principal.getUserId(), termId));
    }

    @PostMapping("/me/resolution-cases")
    @Operation(summary = "Request help resolving a personal timetable clash")
    public ResponseEntity<ResolutionCaseResponse> requestResolution(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody AuraResolutionDtos.CreateResolutionCaseRequest request) {
        return ResponseEntity.ok(resolutionService.requestResolution(
                principal.getUserId(), request));
    }

    @GetMapping("/me/resolution-cases")
    @Operation(summary = "List the signed-in student's resolution cases")
    public ResponseEntity<List<ResolutionCaseResponse>> myResolutionCases(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(resolutionService.listMyCases(
                principal.getUserId(), termId));
    }
}
