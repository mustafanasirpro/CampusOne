package com.campusone.user.controller;

import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.user.dto.request.UpdateProfileRequest;
import com.campusone.user.dto.request.UpdateSkillsRequest;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CurrentUserService currentUserService;

    public UserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated student's profile")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(currentUserService.getCurrentUser(principal.getUserId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated student's profile and preferences")
    public ResponseEntity<CurrentUserResponse> updateCurrentUser(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                currentUserService.updateProfile(principal.getUserId(), request));
    }

    @PutMapping("/me/skills")
    @Operation(summary = "Replace the authenticated student's skills")
    public ResponseEntity<CurrentUserResponse> replaceSkills(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateSkillsRequest request) {
        return ResponseEntity.ok(
                currentUserService.replaceSkills(principal.getUserId(), request));
    }
}
