package com.campusone.aura.controller;

import com.campusone.aura.dto.AuraOperationsDtos;
import com.campusone.aura.service.AuraOperationsService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aura/me")
@Tag(name = "AURA Personal Timetables")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraTimetableViewController {

    private final AuraOperationsService service;

    public AuraTimetableViewController(AuraOperationsService service) {
        this.service = service;
    }

    @GetMapping("/instructor-timetable")
    @Operation(summary = "Get the signed-in instructor's published timetable")
    public ResponseEntity<AuraOperationsDtos.ScopedTimetableResponse> instructorTimetable(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam UUID termId) {
        return ResponseEntity.ok(service.instructorTimetable(principal.getUserId(), termId));
    }
}
