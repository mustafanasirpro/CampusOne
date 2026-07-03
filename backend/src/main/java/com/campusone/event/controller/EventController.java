package com.campusone.event.controller;

import com.campusone.event.dto.request.CreateEventRequest;
import com.campusone.event.dto.request.EventSort;
import com.campusone.event.dto.request.UpdateEventRequest;
import com.campusone.event.dto.response.EventDetailResponse;
import com.campusone.event.dto.response.EventPageResponse;
import com.campusone.event.dto.response.EventParticipantResponse;
import com.campusone.event.entity.EventStatus;
import com.campusone.event.service.EventService;
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
@RequestMapping("/api/v1/events")
@Validated
@Tag(name = "Events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Create a campus event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventDetailResponse> createEvent(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request) {
        EventDetailResponse response = eventService.createEvent(
                principal.getUserId(),
                request);
        return ResponseEntity.created(
                        URI.create("/api/v1/events/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List public campus events")
    public ResponseEntity<EventPageResponse> listEvents(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "UPCOMING") EventSort sort) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(eventService.listPublicEvents(
                viewerUserId,
                status,
                search,
                page,
                size,
                sort));
    }

    @GetMapping("/my")
    @Operation(summary = "List events organized by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventPageResponse> listMyEvents(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "UPCOMING") EventSort sort) {
        return ResponseEntity.ok(eventService.listMyEvents(
                principal.getUserId(),
                page,
                size,
                sort));
    }

    @GetMapping("/joined")
    @Operation(summary = "List events joined by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventPageResponse> listJoinedEvents(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "UPCOMING") EventSort sort) {
        return ResponseEntity.ok(eventService.listJoinedEvents(
                principal.getUserId(),
                page,
                size,
                sort));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get a public or owned private event")
    public ResponseEntity<EventDetailResponse> getEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(eventService.getEvent(
                eventId,
                viewerUserId));
    }

    @PatchMapping("/{eventId}")
    @Operation(summary = "Update an event organized by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventDetailResponse> updateEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(
                principal.getUserId(),
                eventId,
                request));
    }

    @DeleteMapping("/{eventId}")
    @Operation(summary = "Soft-delete an event organized by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        eventService.deleteEvent(principal.getUserId(), eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/participants")
    @Operation(summary = "Join a public upcoming event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventParticipantResponse> joinEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(eventService.joinEvent(
                principal.getUserId(),
                eventId));
    }

    @DeleteMapping("/{eventId}/participants")
    @Operation(summary = "Leave an event joined by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> leaveEvent(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        eventService.leaveEvent(principal.getUserId(), eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/participants/me")
    @Operation(summary = "Get the current user's event participation state")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventParticipantResponse> getParticipantState(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(eventService.getParticipantState(
                principal.getUserId(),
                eventId));
    }
}
