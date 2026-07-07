package com.campusone.marketplace.controller;

import com.campusone.marketplace.dto.request.CreateMarketplaceListingRequest;
import com.campusone.marketplace.dto.request.UpdateMarketplaceListingRequest;
import com.campusone.marketplace.dto.response.MarketplaceListingDetailResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingPageResponse;
import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.service.MarketplaceListingService;
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
@RequestMapping("/api/v1/marketplace/listings")
@Validated
@Tag(name = "Marketplace")
@SecurityRequirement(name = "bearerAuth")
public class MarketplaceListingController {

    private final MarketplaceListingService listingService;

    public MarketplaceListingController(
            MarketplaceListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @Operation(summary = "Create a marketplace listing")
    public ResponseEntity<MarketplaceListingDetailResponse> createListing(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateMarketplaceListingRequest request) {
        MarketplaceListingDetailResponse response =
                listingService.createListing(principal.getUserId(), request);
        return ResponseEntity.created(
                        URI.create("/api/v1/marketplace/listings/" + response.id()))
                .body(response);
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a marketplace listing with uploaded images",
            description = "Accepts a JSON `listing` part and up to the "
                    + "configured number of JPG, PNG, or WebP `images` parts.")
    public ResponseEntity<MarketplaceListingDetailResponse> createListingWithImages(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestPart("listing") CreateMarketplaceListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        MarketplaceListingDetailResponse response =
                listingService.createListingWithUploadedImages(
                        principal.getUserId(),
                        request,
                        images);
        return ResponseEntity.created(
                        URI.create("/api/v1/marketplace/listings/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List active marketplace listings")
    public ResponseEntity<MarketplaceListingPageResponse> listActiveListings(
            @RequestParam(required = false) MarketplaceCategory category,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listingService.listActiveListings(
                category,
                search,
                page,
                size));
    }

    @GetMapping("/my")
    @Operation(summary = "List the authenticated student's marketplace listings")
    public ResponseEntity<MarketplaceListingPageResponse> listMyListings(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listingService.listMyListings(
                principal.getUserId(),
                page,
                size));
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "Get a marketplace listing")
    public ResponseEntity<MarketplaceListingDetailResponse> getListing(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(listingService.getListing(
                listingId,
                viewerUserId));
    }

    @PatchMapping("/{listingId}")
    @Operation(summary = "Update an owned marketplace listing")
    public ResponseEntity<MarketplaceListingDetailResponse> updateListing(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateMarketplaceListingRequest request) {
        return ResponseEntity.ok(listingService.updateListing(
                principal.getUserId(),
                listingId,
                request));
    }

    @PatchMapping(
            path = "/{listingId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an owned marketplace listing and replace images")
    public ResponseEntity<MarketplaceListingDetailResponse> updateListingWithImages(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestPart("listing") UpdateMarketplaceListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(listingService.updateListingWithUploadedImages(
                principal.getUserId(),
                listingId,
                request,
                images));
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "Soft-delete an owned marketplace listing")
    public ResponseEntity<Void> deleteListing(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        listingService.deleteListing(principal.getUserId(), listingId);
        return ResponseEntity.noContent().build();
    }
}
