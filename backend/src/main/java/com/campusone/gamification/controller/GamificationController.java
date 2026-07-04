package com.campusone.gamification.controller;

import com.campusone.gamification.dto.response.BadgeResponse;
import com.campusone.gamification.dto.response.GamificationProfileResponse;
import com.campusone.gamification.dto.response.LeaderboardPageResponse;
import com.campusone.gamification.dto.response.PublicGamificationProfileResponse;
import com.campusone.gamification.dto.response.UserBadgeResponse;
import com.campusone.gamification.dto.response.XpHistoryPageResponse;
import com.campusone.gamification.entity.LeaderboardPeriod;
import com.campusone.gamification.service.GamificationService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gamification")
@Validated
@Tag(name = "Gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(
            GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's gamification profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GamificationProfileResponse> getMyProfile(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(
                gamificationService.getOrCreateProfile(
                        principal.getUserId()));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a public user gamification profile")
    public ResponseEntity<PublicGamificationProfileResponse>
            getPublicProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                gamificationService.getPublicProfile(userId));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get the CampusOne leaderboard")
    public ResponseEntity<LeaderboardPageResponse> leaderboard(
            @RequestParam(defaultValue = "ALL_TIME")
            LeaderboardPeriod period,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50)
            int size) {
        return ResponseEntity.ok(gamificationService.leaderboard(
                period,
                page,
                size));
    }

    @GetMapping("/badges")
    @Operation(summary = "List active CampusOne badges")
    public ResponseEntity<List<BadgeResponse>> listBadges() {
        return ResponseEntity.ok(gamificationService.listBadges());
    }

    @GetMapping("/me/badges")
    @Operation(summary = "List the current user's earned badges")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserBadgeResponse>> getMyBadges(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(gamificationService.getMyBadges(
                principal.getUserId()));
    }

    @GetMapping("/me/xp-history")
    @Operation(summary = "Get the current user's XP history")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<XpHistoryPageResponse> getXpHistory(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size) {
        return ResponseEntity.ok(gamificationService.getXpHistory(
                principal.getUserId(),
                page,
                size));
    }
}
