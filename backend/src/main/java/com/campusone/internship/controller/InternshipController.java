package com.campusone.internship.controller;

import com.campusone.internship.dto.request.CreateInternshipRequest;
import com.campusone.internship.dto.request.InternshipSort;
import com.campusone.internship.dto.request.UpdateInternshipRequest;
import com.campusone.internship.dto.response.InternshipDetailResponse;
import com.campusone.internship.dto.response.InternshipPageResponse;
import com.campusone.internship.dto.response.SavedInternshipResponse;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import com.campusone.internship.service.InternshipService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internships")
@Validated
@Tag(name = "Internships")
public class InternshipController {

    private final InternshipService internshipService;

    public InternshipController(InternshipService internshipService) {
        this.internshipService = internshipService;
    }

    @PostMapping
    @Operation(summary = "Create an internship")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<InternshipDetailResponse> createInternship(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateInternshipRequest request) {
        InternshipDetailResponse response =
                internshipService.createInternship(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(
                        URI.create("/api/v1/internships/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List internships")
    public ResponseEntity<InternshipPageResponse> listInternships(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) InternshipStatus status,
            @RequestParam(required = false) InternshipType internshipType,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") InternshipSort sort) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(internshipService.listInternships(
                viewerUserId,
                status,
                internshipType,
                workMode,
                paid,
                search,
                page,
                size,
                sort));
    }

    @GetMapping("/my")
    @Operation(summary = "List internships posted by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<InternshipPageResponse> listMyInternships(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) InternshipStatus status,
            @RequestParam(required = false) InternshipType internshipType,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") InternshipSort sort) {
        return ResponseEntity.ok(internshipService.listMyInternships(
                principal.getUserId(),
                status,
                internshipType,
                workMode,
                paid,
                search,
                page,
                size,
                sort));
    }

    @GetMapping("/saved")
    @Operation(summary = "List internships saved by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<InternshipPageResponse> listSavedInternships(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") InternshipSort sort) {
        return ResponseEntity.ok(internshipService.listSavedInternships(
                principal.getUserId(),
                page,
                size,
                sort));
    }

    @GetMapping("/{internshipId}")
    @Operation(summary = "Get an internship")
    public ResponseEntity<InternshipDetailResponse> getInternship(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(internshipService.getInternship(
                internshipId,
                viewerUserId));
    }

    @PatchMapping("/{internshipId}")
    @Operation(summary = "Update an internship posted by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<InternshipDetailResponse> updateInternship(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateInternshipRequest request) {
        return ResponseEntity.ok(internshipService.updateInternship(
                principal.getUserId(),
                internshipId,
                request));
    }

    @DeleteMapping("/{internshipId}")
    @Operation(summary = "Soft-delete an owned internship")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteInternship(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        internshipService.deleteInternship(
                principal.getUserId(),
                internshipId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{internshipId}/save")
    @Operation(summary = "Save an internship")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SavedInternshipResponse> saveInternship(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(internshipService.saveInternship(
                principal.getUserId(),
                internshipId));
    }

    @DeleteMapping("/{internshipId}/save")
    @Operation(summary = "Remove a saved internship")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> unsaveInternship(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        internshipService.unsaveInternship(
                principal.getUserId(),
                internshipId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{internshipId}/save/me")
    @Operation(summary = "Get the current user's saved state")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SavedInternshipResponse> getSavedState(
            @PathVariable UUID internshipId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(internshipService.getSavedState(
                principal.getUserId(),
                internshipId));
    }
}
