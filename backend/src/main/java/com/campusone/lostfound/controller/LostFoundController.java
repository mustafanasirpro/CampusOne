package com.campusone.lostfound.controller;

import com.campusone.lostfound.dto.request.CompleteLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.CreateLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.CreateLostFoundItemRequest;
import com.campusone.lostfound.dto.request.ReviewLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.UpdateLostFoundItemRequest;
import com.campusone.lostfound.dto.response.LostFoundClaimPageResponse;
import com.campusone.lostfound.dto.response.LostFoundClaimResponse;
import com.campusone.lostfound.dto.response.LostFoundItemDetailResponse;
import com.campusone.lostfound.dto.response.LostFoundItemPageResponse;
import com.campusone.lostfound.dto.response.LostFoundMatchPageResponse;
import com.campusone.lostfound.dto.response.LostFoundMatchResponse;
import com.campusone.lostfound.dto.response.LostFoundStatsResponse;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.entity.LostFoundMatchStatus;
import com.campusone.lostfound.service.LostFoundService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lost-found")
@Validated
@Tag(name = "Lost & Found")
@SecurityRequirement(name = "bearerAuth")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    public LostFoundController(LostFoundService lostFoundService) {
        this.lostFoundService = lostFoundService;
    }

    @PostMapping(
            path = "/items",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a lost or found item for review")
    public ResponseEntity<LostFoundItemDetailResponse> createItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestPart("item") CreateLostFoundItemRequest request,
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images) {
        LostFoundItemDetailResponse response = lostFoundService.createItem(
                principal.getUserId(),
                request,
                images);
        return ResponseEntity.created(
                        URI.create("/api/v1/lost-found/items/" + response.id()))
                .body(response);
    }

    @GetMapping("/items")
    @Operation(summary = "List same-university published Lost & Found items")
    public ResponseEntity<LostFoundItemPageResponse> listPublishedItems(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) LostFoundItemType type,
            @RequestParam(required = false) LostFoundCategory category,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listPublished(
                principal.getUserId(),
                type,
                category,
                search,
                page,
                size));
    }

    @GetMapping("/items/my")
    @Operation(summary = "List my Lost & Found submissions")
    public ResponseEntity<LostFoundItemPageResponse> listMyItems(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listMyItems(
                principal.getUserId(),
                page,
                size));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get a Lost & Found item")
    public ResponseEntity<LostFoundItemDetailResponse> getItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.getItem(
                principal.getUserId(),
                itemId));
    }

    @PatchMapping(
            path = "/items/{itemId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an owned Lost & Found item")
    public ResponseEntity<LostFoundItemDetailResponse> updateItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId,
            @Valid @RequestPart("item") UpdateLostFoundItemRequest request,
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images) {
        return ResponseEntity.ok(lostFoundService.updateItem(
                principal.getUserId(),
                itemId,
                request,
                images));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Soft-delete an owned Lost & Found item")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        lostFoundService.deleteItem(principal.getUserId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/close")
    @Operation(summary = "Close an owned published Lost & Found item")
    public ResponseEntity<LostFoundItemDetailResponse> closeItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.closeItem(
                principal.getUserId(),
                itemId));
    }

    @PatchMapping("/items/{itemId}/archive")
    @Operation(summary = "Archive an owned Lost & Found item")
    public ResponseEntity<LostFoundItemDetailResponse> archiveItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.archiveItem(
                principal.getUserId(),
                itemId));
    }

    @PatchMapping("/items/{itemId}/renew")
    @Operation(summary = "Renew an owned Lost & Found item for review")
    public ResponseEntity<LostFoundItemDetailResponse> renewItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.renewItem(
                principal.getUserId(),
                itemId));
    }

    @PatchMapping("/items/{itemId}/reopen")
    @Operation(summary = "Reopen an owned closed Lost & Found item")
    public ResponseEntity<LostFoundItemDetailResponse> reopenItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.reopenItem(
                principal.getUserId(),
                itemId));
    }

    @PatchMapping("/items/{itemId}/resolve")
    @Operation(summary = "Mark an owned lost report as recovered")
    public ResponseEntity<LostFoundItemDetailResponse> resolveLostItem(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(lostFoundService.resolveLostItem(
                principal.getUserId(),
                itemId));
    }

    @PostMapping("/items/{itemId}/claims")
    @Operation(summary = "Create a private ownership claim")
    public ResponseEntity<LostFoundClaimResponse> createClaim(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId,
            @Valid @RequestBody CreateLostFoundClaimRequest request) {
        LostFoundClaimResponse response = lostFoundService.createClaim(
                principal.getUserId(),
                itemId,
                request);
        return ResponseEntity.created(
                        URI.create("/api/v1/lost-found/claims/" + response.id()))
                .body(response);
    }

    @GetMapping("/items/{itemId}/claims")
    @Operation(summary = "List claims for an owned Lost & Found item")
    public ResponseEntity<LostFoundClaimPageResponse> listItemClaims(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listItemClaims(
                principal.getUserId(),
                itemId,
                page,
                size));
    }

    @GetMapping("/claims/my")
    @Operation(summary = "List Lost & Found claims related to me")
    public ResponseEntity<LostFoundClaimPageResponse> listMyClaims(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listMyClaims(
                principal.getUserId(),
                page,
                size));
    }

    @PatchMapping("/claims/{claimId}/approve")
    @Operation(summary = "Approve a Lost & Found claim")
    public ResponseEntity<LostFoundClaimResponse> approveClaim(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId,
            @Valid @RequestBody ReviewLostFoundClaimRequest request) {
        return ResponseEntity.ok(lostFoundService.approveClaim(
                principal.getUserId(),
                claimId,
                request));
    }

    @PatchMapping("/claims/{claimId}/reject")
    @Operation(summary = "Reject a Lost & Found claim")
    public ResponseEntity<LostFoundClaimResponse> rejectClaim(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId,
            @Valid @RequestBody ReviewLostFoundClaimRequest request) {
        return ResponseEntity.ok(lostFoundService.rejectClaim(
                principal.getUserId(),
                claimId,
                request));
    }

    @PatchMapping("/claims/{claimId}/cancel")
    @Operation(summary = "Cancel an eligible Lost & Found claim")
    public ResponseEntity<LostFoundClaimResponse> cancelClaim(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId) {
        return ResponseEntity.ok(lostFoundService.cancelClaim(
                principal.getUserId(),
                claimId));
    }

    @PatchMapping("/claims/{claimId}/handover/claimant")
    @Operation(summary = "Confirm claimant-side handover")
    public ResponseEntity<LostFoundClaimResponse> confirmClaimantHandover(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId,
            @RequestBody(required = false) CompleteLostFoundClaimRequest request) {
        return ResponseEntity.ok(lostFoundService.confirmClaimantHandover(
                principal.getUserId(),
                claimId,
                request));
    }

    @PatchMapping("/claims/{claimId}/handover/reporter")
    @Operation(summary = "Confirm reporter-side handover")
    public ResponseEntity<LostFoundClaimResponse> confirmReporterHandover(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId,
            @RequestBody(required = false) CompleteLostFoundClaimRequest request) {
        return ResponseEntity.ok(lostFoundService.confirmReporterHandover(
                principal.getUserId(),
                claimId,
                request));
    }

    @PatchMapping("/claims/{claimId}/complete")
    @Operation(summary = "Mark an approved Lost & Found claim as handed over")
    public ResponseEntity<LostFoundClaimResponse> completeClaim(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID claimId,
            @Valid @RequestBody CompleteLostFoundClaimRequest request) {
        return ResponseEntity.ok(lostFoundService.completeClaim(
                principal.getUserId(),
                claimId,
                request));
    }

    @GetMapping("/matches/my")
    @Operation(summary = "List suggested Lost & Found matches involving me")
    public ResponseEntity<LostFoundMatchPageResponse> listMyMatches(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) LostFoundMatchStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listMyMatches(
                principal.getUserId(),
                status,
                page,
                size));
    }

    @GetMapping("/items/{itemId}/matches")
    @Operation(summary = "List suggested matches for an owned item")
    public ResponseEntity<LostFoundMatchPageResponse> listItemMatches(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID itemId,
            @RequestParam(required = false) LostFoundMatchStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(lostFoundService.listItemMatches(
                principal.getUserId(),
                itemId,
                status,
                page,
                size));
    }

    @PatchMapping("/matches/{matchId}/confirm")
    @Operation(summary = "Confirm a suggested Lost & Found match")
    public ResponseEntity<LostFoundMatchResponse> confirmMatch(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(lostFoundService.confirmMatch(
                principal.getUserId(),
                matchId));
    }

    @PatchMapping("/matches/{matchId}/reject")
    @Operation(summary = "Reject a suggested Lost & Found match")
    public ResponseEntity<LostFoundMatchResponse> rejectMatch(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @PathVariable UUID matchId) {
        return ResponseEntity.ok(lostFoundService.rejectMatch(
                principal.getUserId(),
                matchId));
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Get Lost & Found moderation stats")
    public ResponseEntity<LostFoundStatsResponse> stats(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(lostFoundService.stats(principal.getUserId()));
    }
}
