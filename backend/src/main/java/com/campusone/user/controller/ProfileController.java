package com.campusone.user.controller;

import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.user.dto.response.PublicProfileResponse;
import com.campusone.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Profiles")
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a public student profile")
    public ResponseEntity<PublicProfileResponse> getProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(
                currentUserService.getPublicProfile(userId, viewerUserId));
    }
}
