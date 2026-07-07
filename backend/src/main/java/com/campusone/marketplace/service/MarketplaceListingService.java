package com.campusone.marketplace.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.common.service.CommunityIntegrationService;
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
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.note.storage.ValidatedNoteFile;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MarketplaceListingService {

    private final MarketplaceListingRepository listingRepository;
    private final UserRepository userRepository;
    private final MarketplaceListingMapper listingMapper;
    private final CommunityIntegrationService integrationService;
    private final MarketplaceImageValidator imageValidator;
    private final StorageService storageService;
    private final NoteAdminAuthorizationService adminAuthorizationService;
    private final Clock clock;

    public MarketplaceListingService(
            MarketplaceListingRepository listingRepository,
            UserRepository userRepository,
            MarketplaceListingMapper listingMapper,
            CommunityIntegrationService integrationService,
            MarketplaceImageValidator imageValidator,
            StorageService storageService,
            NoteAdminAuthorizationService adminAuthorizationService,
            Clock clock) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.listingMapper = listingMapper;
        this.integrationService = integrationService;
        this.imageValidator = imageValidator;
        this.storageService = storageService;
        this.adminAuthorizationService = adminAuthorizationService;
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
        applyInitialApprovalState(listing, seller);
        replaceImages(listing, request.images());
        MarketplaceListing savedListing = listingRepository.save(listing);
        integrationService.marketplaceListingCreated(
                userId,
                savedListing.getId());
        return listingMapper.toDetail(savedListing);
    }

    @Transactional
    public MarketplaceListingDetailResponse createListingWithUploadedImages(
            UUID userId,
            CreateMarketplaceListingRequest request,
            List<MultipartFile> imageFiles) {
        User seller = requireUser(userId);
        List<StoredObject> storedImages = uploadImages(userId, imageFiles);
        MarketplaceListing listing = new MarketplaceListing(
                seller,
                request.title(),
                request.description(),
                request.category(),
                request.price(),
                request.currency(),
                request.condition());
        applyInitialApprovalState(listing, seller);
        replaceUploadedImages(listing, storedImages, request.title());
        MarketplaceListing savedListing = listingRepository.save(listing);
        integrationService.marketplaceListingCreated(
                userId,
                savedListing.getId());
        return listingMapper.toDetail(savedListing);
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
    public MarketplaceListingDetailResponse getListing(
            UUID listingId,
            UUID viewerUserId) {
        MarketplaceListing listing = requireListing(listingId);
        requireViewable(listing, viewerUserId);
        return listingMapper.toDetail(listing);
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
        requeueForReviewIfNeeded(listing);
        return listingMapper.toDetail(listing);
    }

    @Transactional
    public MarketplaceListingDetailResponse updateListingWithUploadedImages(
            UUID userId,
            UUID listingId,
            UpdateMarketplaceListingRequest request,
            List<MultipartFile> imageFiles) {
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
        List<StoredObject> storedImages = uploadImages(userId, imageFiles);
        replaceUploadedImages(
                listing,
                storedImages,
                request.title() == null ? listing.getTitle() : request.title());
        requeueForReviewIfNeeded(listing);
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

    private void replaceUploadedImages(
            MarketplaceListing listing,
            List<StoredObject> storedImages,
            String title) {
        listing.clearImages();
        for (int index = 0; index < storedImages.size(); index++) {
            StoredObject storedImage = storedImages.get(index);
            listing.addUploadedImage(
                    storedImage,
                    storageService.createObjectUrl(
                            storedImage.storageProvider(),
                            storedImage.bucketName(),
                            storedImage.objectKey(),
                            storedImage.mimeType(),
                            storedImage.originalFilename()),
                    title + " image " + (index + 1),
                    index);
        }
    }

    private void applyInitialApprovalState(
            MarketplaceListing listing,
            User seller) {
        if (!adminAuthorizationService.canManage(
                seller.getId(),
                seller.getEmail())) {
            listing.submitForReview();
        }
    }

    private void requeueForReviewIfNeeded(MarketplaceListing listing) {
        User seller = listing.getSeller();
        if (!adminAuthorizationService.canManage(
                seller.getId(),
                seller.getEmail())) {
            listing.submitForReview();
        }
    }

    private void requireViewable(
            MarketplaceListing listing,
            UUID viewerUserId) {
        boolean owner = viewerUserId != null && listing.isOwnedBy(viewerUserId);
        if (owner || listing.getStatus() == MarketplaceListingStatus.ACTIVE) {
            return;
        }
        if (viewerUserId != null && isAdmin(viewerUserId)) {
            return;
        }
        throw new ResourceNotFoundException("Marketplace listing");
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> adminAuthorizationService.canManage(
                        user.getId(),
                        user.getEmail()))
                .orElse(false);
    }

    private List<StoredObject> uploadImages(
            UUID userId,
            List<MultipartFile> imageFiles) {
        List<ValidatedNoteFile> validatedImages =
                imageValidator.validate(imageFiles);
        List<StoredObject> storedImages = new ArrayList<>();
        for (ValidatedNoteFile image : validatedImages) {
            storedImages.add(storageService.uploadMarketplaceImage(userId, image));
        }
        registerStorageCleanupOnRollback(storedImages);
        return List.copyOf(storedImages);
    }

    private void registerStorageCleanupOnRollback(List<StoredObject> storedObjects) {
        if (storedObjects.isEmpty()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            return;
                        }
                        storedObjects.forEach(storageService::delete);
                    }
                });
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
