package com.campusone.marketplace.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.marketplace.dto.request.CreateMarketplaceListingRequest;
import com.campusone.marketplace.dto.request.MarketplaceImageRequest;
import com.campusone.marketplace.dto.request.UpdateMarketplaceListingRequest;
import com.campusone.marketplace.dto.response.MarketplaceListingDetailResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingPageResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingSummaryResponse;
import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceListing;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import com.campusone.marketplace.mapper.MarketplaceListingMapper;
import com.campusone.marketplace.repository.MarketplaceListingRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceListingService {

    private final MarketplaceListingRepository listingRepository;
    private final UserRepository userRepository;
    private final MarketplaceListingMapper listingMapper;
    private final Clock clock;

    public MarketplaceListingService(
            MarketplaceListingRepository listingRepository,
            UserRepository userRepository,
            MarketplaceListingMapper listingMapper,
            Clock clock) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.listingMapper = listingMapper;
        this.clock = clock;
    }

    @Transactional
    public MarketplaceListingDetailResponse createListing(
            UUID userId,
            CreateMarketplaceListingRequest request) {
        User seller = requireUser(userId);
        MarketplaceListing listing = new MarketplaceListing(
                seller,
                request.title(),
                request.description(),
                request.category(),
                request.price(),
                request.currency(),
                request.condition());
        replaceImages(listing, request.images());
        return listingMapper.toDetail(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public MarketplaceListingPageResponse listActiveListings(
            MarketplaceCategory category,
            String search,
            int page,
            int size) {
        Page<MarketplaceListing> listings = listingRepository.findActiveListings(
                MarketplaceListingStatus.ACTIVE,
                category,
                toSearchPattern(search),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id"))));
        return toPageResponse(listings);
    }

    @Transactional(readOnly = true)
    public MarketplaceListingDetailResponse getListing(UUID listingId) {
        return listingMapper.toDetail(requireListing(listingId));
    }

    @Transactional(readOnly = true)
    public MarketplaceListingPageResponse listMyListings(
            UUID userId,
            int page,
            int size) {
        Page<MarketplaceListing> listings = listingRepository.findMyListings(
                userId,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id"))));
        return toPageResponse(listings);
    }

    @Transactional
    public MarketplaceListingDetailResponse updateListing(
            UUID userId,
            UUID listingId,
            UpdateMarketplaceListingRequest request) {
        MarketplaceListing listing = requireListing(listingId);
        requireOwner(listing, userId);
        listing.update(
                request.title(),
                request.description(),
                request.category(),
                request.price(),
                request.currency(),
                request.condition(),
                request.status() == null
                        ? null
                        : request.status().toListingStatus());
        if (request.images() != null) {
            replaceImages(listing, request.images());
        }
        return listingMapper.toDetail(listing);
    }

    @Transactional
    public void deleteListing(UUID userId, UUID listingId) {
        MarketplaceListing listing = requireListing(listingId);
        requireOwner(listing, userId);
        listing.softDelete(clock.instant());
    }

    private MarketplaceListingPageResponse toPageResponse(
            Page<MarketplaceListing> page) {
        List<MarketplaceListingSummaryResponse> content = page.getContent().stream()
                .map(listingMapper::toSummary)
                .toList();
        return new MarketplaceListingPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private void replaceImages(
            MarketplaceListing listing,
            List<MarketplaceImageRequest> images) {
        listing.clearImages();
        for (int index = 0; index < images.size(); index++) {
            MarketplaceImageRequest image = images.get(index);
            listing.addImage(
                    image.imageUrl(),
                    image.altText(),
                    index);
        }
    }

    private String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private MarketplaceListing requireListing(UUID listingId) {
        return listingRepository.findDetailedById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marketplace listing"));
    }

    private void requireOwner(
            MarketplaceListing listing,
            UUID userId) {
        if (!listing.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the listing owner may modify this listing.");
        }
    }
}
