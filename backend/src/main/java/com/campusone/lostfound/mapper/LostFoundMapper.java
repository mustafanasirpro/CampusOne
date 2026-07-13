package com.campusone.lostfound.mapper;

import com.campusone.lostfound.dto.response.LostFoundClaimPageResponse;
import com.campusone.lostfound.dto.response.LostFoundClaimResponse;
import com.campusone.lostfound.dto.response.LostFoundImageResponse;
import com.campusone.lostfound.dto.response.LostFoundItemDetailResponse;
import com.campusone.lostfound.dto.response.LostFoundItemPageResponse;
import com.campusone.lostfound.dto.response.LostFoundItemSummaryResponse;
import com.campusone.lostfound.dto.response.LostFoundMatchPageResponse;
import com.campusone.lostfound.dto.response.LostFoundMatchResponse;
import com.campusone.lostfound.dto.response.LostFoundReporterResponse;
import com.campusone.lostfound.entity.LostFoundClaim;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemImage;
import com.campusone.lostfound.entity.LostFoundMatch;
import com.campusone.note.storage.StorageService;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class LostFoundMapper {

    private final StorageService storageService;

    public LostFoundMapper(StorageService storageService) {
        this.storageService = storageService;
    }

    public LostFoundItemSummaryResponse toSummary(LostFoundItem item) {
        return new LostFoundItemSummaryResponse(
                item.getId(),
                item.getType(),
                item.getCategory(),
                item.getTitle(),
                item.getDescription(),
                item.getLocationText(),
                item.getItemDate(),
                item.getBrand(),
                item.getColor(),
                item.getStatus(),
                item.getImages().isEmpty()
                        ? null
                        : toImage(item.getImages().getFirst()),
                toReporter(item.getReporter()),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getPublishedAt(),
                item.getExpiresAt());
    }

    public LostFoundItemDetailResponse toDetail(
            LostFoundItem item,
            UUID viewerUserId,
            Instant now) {
        return new LostFoundItemDetailResponse(
                item.getId(),
                item.getType(),
                item.getCategory(),
                item.getTitle(),
                item.getDescription(),
                item.getLocationText(),
                item.getItemDate(),
                item.getBrand(),
                item.getColor(),
                item.getStatus(),
                item.getModerationReason(),
                toReporter(item.getReporter()),
                item.getImages().stream().map(this::toImage).toList(),
                viewerUserId != null && item.isOwnedBy(viewerUserId),
                viewerUserId != null
                        && !item.isOwnedBy(viewerUserId)
                        && item.isClaimableAt(now),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getPublishedAt(),
                item.getExpiresAt(),
                item.getResolvedAt());
    }

    public LostFoundImageResponse toImage(LostFoundItemImage image) {
        return new LostFoundImageResponse(
                image.getId(),
                storageService.createObjectUrl(
                        image.getStorageProvider(),
                        image.getBucketName(),
                        image.getObjectKey(),
                        image.getMimeType(),
                        image.getOriginalFilename()),
                image.getOriginalFilename(),
                image.getMimeType(),
                image.getFileSizeBytes(),
                image.getDisplayOrder());
    }

    public LostFoundClaimResponse toClaim(
            LostFoundClaim claim,
            boolean includeProof) {
        return new LostFoundClaimResponse(
                claim.getId(),
                claim.getItem().getId(),
                claim.getItem().getTitle(),
                toReporter(claim.getClaimant()),
                includeProof ? claim.getProofText() : null,
                claim.getStatus(),
                claim.getReviewerNote(),
                claim.getHandoverNote(),
                claim.getCreatedAt(),
                claim.getReviewedAt(),
                claim.getReporterHandoverConfirmedAt(),
                claim.getClaimantHandoverConfirmedAt(),
                claim.getHandoverCompletedAt());
    }

    public LostFoundMatchResponse toMatch(LostFoundMatch match) {
        return new LostFoundMatchResponse(
                match.getId(),
                toSummary(match.getLostItem()),
                toSummary(match.getFoundItem()),
                match.getScore(),
                match.getReasons(),
                match.getStatus(),
                match.getCreatedAt(),
                match.getUpdatedAt());
    }

    public LostFoundItemPageResponse toItemPage(Page<LostFoundItem> page) {
        List<LostFoundItemSummaryResponse> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new LostFoundItemPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    public LostFoundClaimPageResponse toClaimPage(
            Page<LostFoundClaim> page,
            UUID viewerUserId) {
        List<LostFoundClaimResponse> content = page.getContent().stream()
                .map(claim -> toClaim(
                        claim,
                        claim.isClaimant(viewerUserId)
                                || claim.getItem().isOwnedBy(viewerUserId)))
                .toList();
        return new LostFoundClaimPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    public LostFoundMatchPageResponse toMatchPage(Page<LostFoundMatch> page) {
        List<LostFoundMatchResponse> content = page.getContent().stream()
                .map(this::toMatch)
                .toList();
        return new LostFoundMatchPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private LostFoundReporterResponse toReporter(User user) {
        StudentProfile profile = user.getStudentProfile();
        if (profile == null
                || profile.getVisibility() != ProfileVisibility.PUBLIC) {
            return new LostFoundReporterResponse(user.getId(), null, null);
        }
        return new LostFoundReporterResponse(
                user.getId(),
                profile.getFullName(),
                profile.getAvatarUrl());
    }
}
