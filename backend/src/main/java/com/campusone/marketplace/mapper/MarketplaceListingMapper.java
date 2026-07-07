package com.campusone.marketplace.mapper;

import com.campusone.marketplace.dto.response.MarketplaceImageResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingDetailResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingSummaryResponse;
import com.campusone.marketplace.dto.response.MarketplaceSellerResponse;
import com.campusone.marketplace.entity.MarketplaceListing;
import com.campusone.marketplace.entity.MarketplaceListingImage;
import com.campusone.note.storage.StorageService;
import com.campusone.user.entity.StudentProfile;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceListingMapper {

    private final StorageService storageService;

    public MarketplaceListingMapper(StorageService storageService) {
        this.storageService = storageService;
    }

    public MarketplaceListingSummaryResponse toSummary(
            MarketplaceListing listing) {
        MarketplaceImageResponse primaryImage = listing.getImages().stream()
                .min(Comparator.comparingInt(MarketplaceListingImage::getDisplayOrder))
                .map(this::toImage)
                .orElse(null);
        return new MarketplaceListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCategory(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getCondition(),
                listing.getStatus(),
                primaryImage,
                toSeller(listing),
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }

    public MarketplaceListingDetailResponse toDetail(
            MarketplaceListing listing) {
        List<MarketplaceImageResponse> images = listing.getImages().stream()
                .sorted(Comparator.comparingInt(
                        MarketplaceListingImage::getDisplayOrder))
                .map(this::toImage)
                .toList();
        return new MarketplaceListingDetailResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getCondition(),
                listing.getStatus(),
                images,
                toSeller(listing),
                listing.getCreatedAt(),
                listing.getUpdatedAt());
    }

    private MarketplaceImageResponse toImage(MarketplaceListingImage image) {
        return new MarketplaceImageResponse(
                image.getId(),
                imageUrl(image),
                image.getAltText(),
                image.getDisplayOrder(),
                image.getOriginalFilename(),
                image.getMimeType(),
                image.getSizeBytes());
    }

    private String imageUrl(MarketplaceListingImage image) {
        if (image.getObjectKey() == null) {
            return image.getImageUrl();
        }
        try {
            return storageService.createObjectUrl(
                    image.getStorageProvider(),
                    image.getBucketName(),
                    image.getObjectKey(),
                    image.getMimeType(),
                    image.getOriginalFilename());
        } catch (RuntimeException exception) {
            return image.getImageUrl();
        }
    }

    private MarketplaceSellerResponse toSeller(MarketplaceListing listing) {
        StudentProfile profile = listing.getSeller().getStudentProfile();
        return new MarketplaceSellerResponse(
                listing.getSeller().getId(),
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getUniversity().getName());
    }
}
