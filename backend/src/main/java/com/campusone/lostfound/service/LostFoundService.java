package com.campusone.lostfound.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.common.service.CommunityIntegrationService;
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
import com.campusone.lostfound.dto.response.LostFoundStatsResponse;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundClaim;
import com.campusone.lostfound.entity.LostFoundClaimStatus;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemImage;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.entity.LostFoundMatch;
import com.campusone.lostfound.entity.LostFoundMatchStatus;
import com.campusone.lostfound.exception.LostFoundConflictException;
import com.campusone.lostfound.mapper.LostFoundMapper;
import com.campusone.lostfound.repository.LostFoundClaimRepository;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.repository.LostFoundMatchRepository;
import com.campusone.moderation.service.ModeratorAuthorizationService;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.note.storage.ValidatedNoteFile;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LostFoundService {

    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(90);

    private final LostFoundItemRepository itemRepository;
    private final LostFoundClaimRepository claimRepository;
    private final LostFoundMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final LostFoundMapper mapper;
    private final LostFoundImageValidator imageValidator;
    private final StorageService storageService;
    private final LostFoundMatchingService matchingService;
    private final CommunityIntegrationService integrationService;
    private final NoteAdminAuthorizationService adminAuthorizationService;
    private final ModeratorAuthorizationService moderatorAuthorizationService;
    private final Clock clock;

    public LostFoundService(
            LostFoundItemRepository itemRepository,
            LostFoundClaimRepository claimRepository,
            LostFoundMatchRepository matchRepository,
            UserRepository userRepository,
            LostFoundMapper mapper,
            LostFoundImageValidator imageValidator,
            StorageService storageService,
            LostFoundMatchingService matchingService,
            CommunityIntegrationService integrationService,
            NoteAdminAuthorizationService adminAuthorizationService,
            ModeratorAuthorizationService moderatorAuthorizationService,
            Clock clock) {
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.imageValidator = imageValidator;
        this.storageService = storageService;
        this.matchingService = matchingService;
        this.integrationService = integrationService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.moderatorAuthorizationService = moderatorAuthorizationService;
        this.clock = clock;
    }

    @Transactional
    public LostFoundItemDetailResponse createItem(
            UUID userId,
            CreateLostFoundItemRequest request,
            List<MultipartFile> images) {
        User reporter = requireUser(userId);
        StudentProfile profile = requireProfile(reporter);
        List<StoredObject> storedImages = uploadImages(userId, images);
        LostFoundItem item = new LostFoundItem(
                reporter,
                profile.getUniversity(),
                request.type(),
                request.category(),
                request.title(),
                request.description(),
                request.locationText(),
                request.itemDate(),
                request.brand(),
                request.color());
        item.replaceImages(toImages(storedImages));
        LostFoundItem saved = itemRepository.save(item);
        integrationService.lostFoundItemSubmittedForApproval(
                userId,
                saved.getId(),
                saved.getTitle());
        return mapper.toDetail(saved, userId, clock.instant());
    }

    @Transactional(readOnly = true)
    public LostFoundItemPageResponse listPublished(
            UUID userId,
            LostFoundItemType type,
            LostFoundCategory category,
            String search,
            int page,
            int size) {
        User user = requireUser(userId);
        StudentProfile profile = requireProfile(user);
        return mapper.toItemPage(itemRepository.findPublishedForUniversity(
                profile.getUniversity().getId(),
                LostFoundItemStatus.PUBLISHED,
                clock.instant(),
                type,
                category,
                toSearchPattern(search),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id")))));
    }

    @Transactional(readOnly = true)
    public LostFoundItemPageResponse listMyItems(
            UUID userId,
            int page,
            int size) {
        return mapper.toItemPage(itemRepository.findOwnedItems(
                userId,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id")))));
    }

    @Transactional(readOnly = true)
    public LostFoundItemDetailResponse getItem(
            UUID userId,
            UUID itemId) {
        User viewer = requireUser(userId);
        LostFoundItem item = requireItem(itemId);
        if (!isAdmin(userId)) {
            StudentProfile profile = requireProfile(viewer);
            requireSameUniversity(item, profile);
        }
        requireViewable(item, userId);
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundItemDetailResponse updateItem(
            UUID userId,
            UUID itemId,
            UpdateLostFoundItemRequest request,
            List<MultipartFile> images) {
        User user = requireUser(userId);
        StudentProfile profile = requireProfile(user);
        LostFoundItem item = requireItem(itemId);
        requireSameUniversity(item, profile);
        requireOwner(item, userId);
        if (EnumSet.of(
                LostFoundItemStatus.RESOLVED,
                LostFoundItemStatus.CLOSED,
                LostFoundItemStatus.ARCHIVED,
                LostFoundItemStatus.DELETED)
                .contains(item.getStatus())) {
            throw new LostFoundConflictException(
                    "This item can no longer be edited.");
        }
        if (request.type() != null && request.type() != item.getType()) {
            throw new LostFoundConflictException(
                    "Lost and found item type cannot be changed.");
        }

        boolean hasMetadataChanges = hasMetadataChanges(request);
        item.update(
                request.category(),
                request.title(),
                request.description(),
                request.locationText(),
                request.itemDate(),
                request.brand(),
                request.color());
        if (images != null) {
            List<StoredObject> previousImages = item.getImages().stream()
                    .map(this::toStoredObject)
                    .toList();
            List<StoredObject> storedImages = uploadImages(userId, images);
            item.replaceImages(toImages(storedImages));
            registerStorageCleanupAfterCommit(previousImages);
            hasMetadataChanges = true;
        }
        if (hasMetadataChanges) {
            item.resubmitForReview();
            integrationService.lostFoundItemSubmittedForApproval(
                    userId,
                    item.getId(),
                    item.getTitle());
        }
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public void deleteItem(UUID userId, UUID itemId) {
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        requireNoApprovedClaim(item);
        List<StoredObject> imagesToDelete = item.getImages().stream()
                .map(this::toStoredObject)
                .toList();
        item.softDelete(clock.instant());
        registerStorageCleanupAfterCommit(imagesToDelete);
    }

    @Transactional
    public LostFoundItemDetailResponse closeItem(UUID userId, UUID itemId) {
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        requireNoApprovedClaim(item);
        if (item.getStatus() != LostFoundItemStatus.PUBLISHED) {
            throw new LostFoundConflictException(
                    "Only a published item can be closed.");
        }
        item.close();
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundItemDetailResponse archiveItem(UUID userId, UUID itemId) {
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        requireNoApprovedClaim(item);
        if (!EnumSet.of(
                LostFoundItemStatus.PUBLISHED,
                LostFoundItemStatus.CLOSED)
                .contains(item.getStatus())) {
            throw new LostFoundConflictException(
                    "This item cannot be archived right now.");
        }
        item.archive();
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundItemDetailResponse renewItem(UUID userId, UUID itemId) {
        User user = requireUser(userId);
        StudentProfile profile = requireProfile(user);
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        requireSameUniversity(item, profile);
        requireNoApprovedClaim(item);
        if (EnumSet.of(
                LostFoundItemStatus.RESOLVED,
                LostFoundItemStatus.DELETED)
                .contains(item.getStatus())) {
            throw new LostFoundConflictException(
                    "This item cannot be renewed.");
        }
        item.resubmitForReview();
        integrationService.lostFoundItemSubmittedForApproval(
                userId,
                item.getId(),
                item.getTitle());
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundItemDetailResponse reopenItem(UUID userId, UUID itemId) {
        User user = requireUser(userId);
        StudentProfile profile = requireProfile(user);
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        requireSameUniversity(item, profile);
        requireNoApprovedClaim(item);
        if (item.getStatus() != LostFoundItemStatus.CLOSED) {
            throw new LostFoundConflictException(
                    "Only a closed item can be reopened.");
        }
        if (item.getPublishedAt() != null
                && (item.getExpiresAt() == null
                        || item.getExpiresAt().isAfter(clock.instant()))) {
            item.reopenPublished();
        } else {
            item.resubmitForReview();
            integrationService.lostFoundItemSubmittedForApproval(
                    userId,
                    item.getId(),
                    item.getTitle());
        }
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundItemDetailResponse resolveLostItem(UUID userId, UUID itemId) {
        LostFoundItem item = requireItem(itemId);
        requireOwner(item, userId);
        if (item.getType() != LostFoundItemType.LOST) {
            throw new LostFoundConflictException(
                    "Only lost reports can be marked recovered by the owner.");
        }
        if (item.getStatus() != LostFoundItemStatus.PUBLISHED) {
            throw new LostFoundConflictException(
                    "Only a published lost report can be marked recovered.");
        }
        Instant now = clock.instant();
        item.resolve(now);
        rejectRemainingPendingClaims(
                item,
                null,
                requireUser(userId),
                now);
        return mapper.toDetail(item, userId, clock.instant());
    }

    @Transactional
    public LostFoundClaimResponse createClaim(
            UUID userId,
            UUID itemId,
            CreateLostFoundClaimRequest request) {
        User claimant = requireUser(userId);
        StudentProfile profile = requireProfile(claimant);
        LostFoundItem item = itemRepository.findActiveByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found item"));
        requireSameUniversity(item, profile);
        if (item.isOwnedBy(userId)) {
            throw new LostFoundConflictException(
                    "You cannot claim your own item.");
        }
        if (!item.isClaimableAt(clock.instant())) {
            throw new LostFoundConflictException(
                    "This item is not accepting claims.");
        }
        if (claimRepository.existsByItemIdAndClaimantIdAndStatusIn(
                itemId,
                userId,
                EnumSet.of(
                        LostFoundClaimStatus.PENDING,
                        LostFoundClaimStatus.APPROVED))) {
            throw new LostFoundConflictException(
                    "You already have an active claim for this item.");
        }
        LostFoundClaim claim = claimRepository.save(new LostFoundClaim(
                item,
                claimant,
                request.proofText()));
        integrationService.lostFoundClaimCreated(
                item.getReporter().getId(),
                userId,
                item.getId(),
                claim.getId(),
                item.getTitle());
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional(readOnly = true)
    public LostFoundClaimPageResponse listMyClaims(
            UUID userId,
            int page,
            int size) {
        return mapper.toClaimPage(
                claimRepository.findRelatedToUser(
                        userId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.asc("id")))),
                userId);
    }

    @Transactional(readOnly = true)
    public LostFoundClaimPageResponse listItemClaims(
            UUID userId,
            UUID itemId,
            int page,
            int size) {
        LostFoundItem item = requireItem(itemId);
        if (!item.isOwnedBy(userId) && !isAdmin(userId)) {
            throw new AccessDeniedException(
                    "Only the reporter or an admin can review claims.");
        }
        return mapper.toClaimPage(
                claimRepository.findForItem(
                        itemId,
                        PageRequest.of(page, size)),
                userId);
    }

    @Transactional
    public LostFoundClaimResponse approveClaim(
            UUID userId,
            UUID claimId,
            ReviewLostFoundClaimRequest request) {
        LostFoundClaim claim = requireClaim(claimId);
        requireClaimReviewer(claim, userId);
        if (claim.getStatus() != LostFoundClaimStatus.PENDING) {
            throw new LostFoundConflictException(
                    "Only a pending claim can be approved.");
        }
        if (!claim.getItem().isPubliclyVisibleAt(clock.instant())) {
            throw new LostFoundConflictException(
                    "This item is no longer accepting claim approvals.");
        }
        if (claimRepository.existsByItemIdAndStatusIn(
                claim.getItem().getId(),
                EnumSet.of(LostFoundClaimStatus.APPROVED))) {
            throw new LostFoundConflictException(
                    "This item already has an approved claim.");
        }
        User reviewer = requireUser(userId);
        claim.approve(reviewer, request.note(), clock.instant());
        claim.getItem().startClaim();
        integrationService.lostFoundClaimReviewed(
                claim.getClaimant().getId(),
                reviewer.getId(),
                claim.getItem().getId(),
                claim.getId(),
                claim.getItem().getTitle(),
                true);
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional
    public LostFoundClaimResponse rejectClaim(
            UUID userId,
            UUID claimId,
            ReviewLostFoundClaimRequest request) {
        LostFoundClaim claim = requireClaim(claimId);
        requireClaimReviewer(claim, userId);
        if (claim.getStatus() != LostFoundClaimStatus.PENDING) {
            throw new LostFoundConflictException(
                    "Only a pending claim can be rejected.");
        }
        User reviewer = requireUser(userId);
        claim.reject(reviewer, request.note(), clock.instant());
        if (!claimRepository.existsByItemIdAndStatusIn(
                claim.getItem().getId(),
                EnumSet.of(
                        LostFoundClaimStatus.PENDING,
                        LostFoundClaimStatus.APPROVED))) {
            claim.getItem().reopenAfterRejectedClaim();
        }
        integrationService.lostFoundClaimReviewed(
                claim.getClaimant().getId(),
                reviewer.getId(),
                claim.getItem().getId(),
                claim.getId(),
                claim.getItem().getTitle(),
                false);
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional
    public LostFoundClaimResponse cancelClaim(UUID userId, UUID claimId) {
        LostFoundClaim claim = requireClaim(claimId);
        boolean canCancelPending = claim.getStatus() == LostFoundClaimStatus.PENDING
                && claim.isClaimant(userId);
        boolean canCancelApproved = claim.getStatus() == LostFoundClaimStatus.APPROVED
                && (claim.isClaimant(userId)
                        || claim.getItem().isOwnedBy(userId)
                        || isAdmin(userId))
                && claim.getReporterHandoverConfirmedAt() == null
                && claim.getClaimantHandoverConfirmedAt() == null;
        if (!canCancelPending && !canCancelApproved) {
            throw new AccessDeniedException(
                    "This claim cannot be cancelled by the current user.");
        }
        claim.cancel();
        if (!claimRepository.existsByItemIdAndStatusIn(
                claim.getItem().getId(),
                EnumSet.of(LostFoundClaimStatus.APPROVED))) {
            claim.getItem().reopenAfterRejectedClaim();
        }
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional
    public LostFoundClaimResponse confirmClaimantHandover(
            UUID userId,
            UUID claimId,
            CompleteLostFoundClaimRequest request) {
        LostFoundClaim claim = requireClaim(claimId);
        if (!claim.isClaimant(userId)) {
            throw new AccessDeniedException(
                    "Only the claimant can confirm claimant handover.");
        }
        confirmHandover(claim, request, false, userId);
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional
    public LostFoundClaimResponse confirmReporterHandover(
            UUID userId,
            UUID claimId,
            CompleteLostFoundClaimRequest request) {
        LostFoundClaim claim = requireClaim(claimId);
        if (!claim.getItem().isOwnedBy(userId) && !isAdmin(userId)) {
            throw new AccessDeniedException(
                    "Only the reporter or an admin can confirm reporter handover.");
        }
        confirmHandover(claim, request, true, userId);
        return mapper.toClaim(claim, true, userId);
    }

    @Transactional
    public LostFoundClaimResponse completeClaim(
            UUID userId,
            UUID claimId,
            CompleteLostFoundClaimRequest request) {
        LostFoundClaim claim = requireClaim(claimId);
        requireClaimReviewer(claim, userId);
        if (claim.getStatus() != LostFoundClaimStatus.APPROVED) {
            throw new LostFoundConflictException(
                    "Only an approved claim can be completed.");
        }
        Instant now = clock.instant();
        claim.complete(request.handoverNote(), now);
        claim.getItem().resolve(now);
        rejectRemainingPendingClaims(
                claim.getItem(),
                claim.getId(),
                requireUser(userId),
                now);
        integrationService.lostFoundClaimCompleted(
                claim.getClaimant().getId(),
                claim.getItem().getReporter().getId(),
                claim.getItem().getId(),
                claim.getId(),
                claim.getItem().getTitle());
        return mapper.toClaim(claim, true, userId);
    }

    private void confirmHandover(
            LostFoundClaim claim,
            CompleteLostFoundClaimRequest request,
            boolean reporterSide,
            UUID confirmerUserId) {
        if (claim.getStatus() != LostFoundClaimStatus.APPROVED) {
            throw new LostFoundConflictException(
                    "Only an approved claim can be confirmed.");
        }
        Instant now = clock.instant();
        if (reporterSide) {
            claim.confirmReporterHandover(now);
        } else {
            claim.confirmClaimantHandover(now);
        }
        if (claim.hasBothHandoverConfirmations()) {
            claim.complete(
                    request == null ? null : request.handoverNote(),
                    now);
            claim.getItem().resolve(now);
            rejectRemainingPendingClaims(
                    claim.getItem(),
                    claim.getId(),
                    requireUser(confirmerUserId),
                    now);
            integrationService.lostFoundClaimCompleted(
                    claim.getClaimant().getId(),
                    claim.getItem().getReporter().getId(),
                    claim.getItem().getId(),
                    claim.getId(),
                    claim.getItem().getTitle());
        }
    }

    @Transactional(readOnly = true)
    public LostFoundMatchPageResponse listMyMatches(
            UUID userId,
            LostFoundMatchStatus status,
            int page,
            int size) {
        return mapper.toMatchPage(matchRepository.findRelatedToUser(
                userId,
                status,
                PageRequest.of(page, size)));
    }

    @Transactional(readOnly = true)
    public LostFoundMatchPageResponse listItemMatches(
            UUID userId,
            UUID itemId,
            LostFoundMatchStatus status,
            int page,
            int size) {
        LostFoundItem item = requireItem(itemId);
        if (!item.isOwnedBy(userId) && !isAdmin(userId)) {
            throw new AccessDeniedException(
                    "Only involved users can view suggested matches.");
        }
        return mapper.toMatchPage(matchRepository.findForItem(
                itemId,
                status,
                PageRequest.of(page, size)));
    }

    @Transactional
    public com.campusone.lostfound.dto.response.LostFoundMatchResponse
            confirmMatch(UUID userId, UUID matchId) {
        LostFoundMatch match = requireMatch(matchId);
        requireMatchParticipant(match, userId);
        User user = requireUser(userId);
        match.confirm(user, clock.instant());
        return mapper.toMatch(match);
    }

    @Transactional
    public com.campusone.lostfound.dto.response.LostFoundMatchResponse
            rejectMatch(UUID userId, UUID matchId) {
        LostFoundMatch match = requireMatch(matchId);
        requireMatchParticipant(match, userId);
        User user = requireUser(userId);
        match.reject(user, clock.instant());
        return mapper.toMatch(match);
    }

    @Transactional(readOnly = true)
    public LostFoundStatsResponse stats(UUID adminUserId) {
        moderatorAuthorizationService.requireActiveModerator(adminUserId);
        Map<String, Long> counts = new LinkedHashMap<>();
        itemRepository.countByStatus().forEach(row ->
                counts.put(String.valueOf(row[0]), (Long) row[1]));
        return new LostFoundStatsResponse(counts);
    }

    @Transactional
    public com.campusone.moderation.dto.response.PendingApprovalItemResponse
            approveItem(
                    User admin,
                    UUID itemId) {
        LostFoundItem item = itemRepository.findActiveByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found item"));
        item.approve(admin, clock.instant(), clock.instant().plus(DEFAULT_EXPIRY));
        matchingService.suggestMatchesFor(item);
        return toPendingApprovalItem(item);
    }

    @Transactional
    public com.campusone.moderation.dto.response.PendingApprovalItemResponse
            rejectItem(
                    User admin,
                    UUID itemId,
                    String reason) {
        LostFoundItem item = itemRepository.findActiveByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found item"));
        item.reject(admin, reason, clock.instant());
        return toPendingApprovalItem(item);
    }

    public com.campusone.moderation.dto.response.PendingApprovalItemResponse
            toPendingApprovalItem(LostFoundItem item) {
        return new com.campusone.moderation.dto.response.PendingApprovalItemResponse(
                item.getId(),
                com.campusone.moderation.entity.ModerationTargetType.LOST_FOUND_ITEM,
                item.getTitle(),
                item.getDescription(),
                new com.campusone.moderation.dto.response.ReporterSummaryResponse(
                        item.getReporter().getId(),
                        item.getReporter().getStudentProfile() == null
                                ? null
                                : item.getReporter().getStudentProfile().getFullName()),
                item.getCreatedAt(),
                item.getStatus().name(),
                item.getImages().isEmpty()
                        ? null
                        : mapper.toImage(item.getImages().getFirst()).imageUrl(),
                "/lost-found/" + item.getId());
    }

    private List<StoredObject> uploadImages(
            UUID userId,
            List<MultipartFile> imageFiles) {
        List<ValidatedNoteFile> validatedImages =
                imageValidator.validate(imageFiles);
        List<StoredObject> storedImages = new ArrayList<>();
        try {
            for (ValidatedNoteFile image : validatedImages) {
                storedImages.add(storageService.uploadLostFoundImage(userId, image));
            }
        } catch (RuntimeException exception) {
            storedImages.forEach(storageService::delete);
            throw exception;
        }
        registerStorageCleanupOnRollback(storedImages);
        return List.copyOf(storedImages);
    }

    private List<LostFoundItemImage> toImages(List<StoredObject> storedImages) {
        List<LostFoundItemImage> images = new ArrayList<>();
        for (int index = 0; index < storedImages.size(); index++) {
            images.add(new LostFoundItemImage(storedImages.get(index), index));
        }
        return images;
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
                        if (status != STATUS_COMMITTED) {
                            storedObjects.forEach(storageService::delete);
                        }
                    }
                });
    }

    private void registerStorageCleanupAfterCommit(List<StoredObject> storedObjects) {
        if (storedObjects.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storedObjects.forEach(storageService::delete);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        storedObjects.forEach(storageService::delete);
                    }
                });
    }

    private StoredObject toStoredObject(LostFoundItemImage image) {
        return new StoredObject(
                image.getStorageProvider(),
                image.getBucketName(),
                image.getObjectKey(),
                image.getOriginalFilename(),
                image.getMimeType(),
                image.getFileSizeBytes(),
                image.getChecksumSha256());
    }

    private void rejectRemainingPendingClaims(
            LostFoundItem item,
            UUID completedClaimId,
            User reviewer,
            Instant now) {
        claimRepository.findByItemIdAndStatus(
                        item.getId(),
                        LostFoundClaimStatus.PENDING)
                .stream()
                .filter(claim -> !claim.getId().equals(completedClaimId))
                .forEach(claim -> claim.reject(
                        reviewer,
                        "Item was resolved.",
                        now));
    }

    private boolean hasMetadataChanges(UpdateLostFoundItemRequest request) {
        return request.category() != null
                || request.title() != null
                || request.description() != null
                || request.locationText() != null
                || request.itemDate() != null
                || request.brand() != null
                || request.color() != null;
    }

    private LostFoundItem requireItem(UUID itemId) {
        return itemRepository.findDetailedById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found item"));
    }

    private LostFoundClaim requireClaim(UUID claimId) {
        return claimRepository.findDetailedById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found claim"));
    }

    private LostFoundMatch requireMatch(UUID matchId) {
        return matchRepository.findDetailedById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lost and found match"));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private StudentProfile requireProfile(User user) {
        StudentProfile profile = user.getStudentProfile();
        if (profile == null || profile.getUniversity() == null) {
            throw new LostFoundConflictException(
                    "Complete your student profile before using Lost & Found.");
        }
        return profile;
    }

    private void requireSameUniversity(
            LostFoundItem item,
            StudentProfile profile) {
        if (!item.isSameUniversity(profile.getUniversity().getId())) {
            throw new ResourceNotFoundException("Lost and found item");
        }
    }

    private void requireViewable(LostFoundItem item, UUID userId) {
        if (item.isOwnedBy(userId)
                || item.isPubliclyVisibleAt(clock.instant())
                || isAdmin(userId)) {
            return;
        }
        throw new ResourceNotFoundException("Lost and found item");
    }

    private void requireOwner(LostFoundItem item, UUID userId) {
        if (!item.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the reporter can edit this Lost & Found item.");
        }
    }

    private void requireNoApprovedClaim(LostFoundItem item) {
        if (claimRepository.existsByItemIdAndStatusIn(
                item.getId(),
                EnumSet.of(LostFoundClaimStatus.APPROVED))) {
            throw new LostFoundConflictException(
                    "Resolve or cancel the approved claim before changing this item.");
        }
    }

    private void requireClaimReviewer(LostFoundClaim claim, UUID userId) {
        if (claim.getItem().isOwnedBy(userId) || isAdmin(userId)) {
            return;
        }
        throw new AccessDeniedException(
                "Only the reporter or an admin can review this claim.");
    }

    private void requireMatchParticipant(LostFoundMatch match, UUID userId) {
        if (match.involvesUser(userId) || isAdmin(userId)) {
            return;
        }
        throw new AccessDeniedException(
                "Only involved users can update this match.");
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> adminAuthorizationService.canManage(
                        user.getId(),
                        user.getEmail()))
                .orElse(false);
    }

    private String toSearchPattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
