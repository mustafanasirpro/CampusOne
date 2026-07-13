package com.campusone.moderation.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.discussion.entity.DiscussionQuestion;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import com.campusone.discussion.repository.DiscussionQuestionRepository;
import com.campusone.event.entity.CampusEvent;
import com.campusone.event.entity.EventStatus;
import com.campusone.event.repository.CampusEventRepository;
import com.campusone.internship.entity.Internship;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.repository.InternshipRepository;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.marketplace.entity.MarketplaceListing;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import com.campusone.marketplace.repository.MarketplaceListingRepository;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.service.LostFoundService;
import com.campusone.moderation.dto.response.PendingApprovalItemResponse;
import com.campusone.moderation.dto.response.PendingApprovalPageResponse;
import com.campusone.moderation.dto.response.ReporterSummaryResponse;
import com.campusone.moderation.entity.ModerationAction;
import com.campusone.moderation.entity.ModerationActionType;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.exception.ModerationConflictException;
import com.campusone.moderation.repository.ModerationActionRepository;
import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.repository.NoteRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentApprovalService {

    private static final int COMBINED_FETCH_LIMIT = 200;

    private final NoteRepository noteRepository;
    private final MarketplaceListingRepository listingRepository;
    private final CampusEventRepository eventRepository;
    private final DiscussionQuestionRepository questionRepository;
    private final InternshipRepository internshipRepository;
    private final LostFoundItemRepository lostFoundItemRepository;
    private final ModerationActionRepository actionRepository;
    private final UserRepository userRepository;
    private final NoteAdminAuthorizationService adminAuthorizationService;
    private final ModeratorAuthorizationService moderatorAuthorizationService;
    private final CommunityIntegrationService integrationService;
    private final LostFoundService lostFoundService;
    private final Clock clock;

    public ContentApprovalService(
            NoteRepository noteRepository,
            MarketplaceListingRepository listingRepository,
            CampusEventRepository eventRepository,
            DiscussionQuestionRepository questionRepository,
            InternshipRepository internshipRepository,
            LostFoundItemRepository lostFoundItemRepository,
            ModerationActionRepository actionRepository,
            UserRepository userRepository,
            NoteAdminAuthorizationService adminAuthorizationService,
            ModeratorAuthorizationService moderatorAuthorizationService,
            CommunityIntegrationService integrationService,
            LostFoundService lostFoundService,
            Clock clock) {
        this.noteRepository = noteRepository;
        this.listingRepository = listingRepository;
        this.eventRepository = eventRepository;
        this.questionRepository = questionRepository;
        this.internshipRepository = internshipRepository;
        this.lostFoundItemRepository = lostFoundItemRepository;
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
        this.moderatorAuthorizationService = moderatorAuthorizationService;
        this.integrationService = integrationService;
        this.lostFoundService = lostFoundService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PendingApprovalPageResponse listPending(
            UUID adminUserId,
            ModerationTargetType targetType,
            int page,
            int size) {
        requireAdmin(adminUserId);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<PendingApprovalItemResponse> pendingPage =
                targetType == null
                        ? combinedPendingPage(pageRequest)
                        : pendingPageForTarget(targetType, pageRequest);
        return new PendingApprovalPageResponse(
                pendingPage.getContent(),
                pendingPage.getNumber(),
                pendingPage.getSize(),
                pendingPage.getTotalElements(),
                pendingPage.getTotalPages(),
                pendingPage.isFirst(),
                pendingPage.isLast());
    }

    @Transactional
    public PendingApprovalItemResponse approve(
            UUID adminUserId,
            ModerationTargetType targetType,
            UUID targetId) {
        User admin = requireApprover(adminUserId, targetType);
        PendingApprovalItemResponse response = switch (targetType) {
            case NOTE -> approveNote(admin, targetId);
            case MARKETPLACE_LISTING -> approveListing(targetId);
            case EVENT -> approveEvent(targetId);
            case DISCUSSION_QUESTION -> approveQuestion(targetId);
            case INTERNSHIP -> approveInternship(targetId);
            case LOST_FOUND_ITEM -> lostFoundService.approveItem(admin, targetId);
            default -> throw unsupportedTarget();
        };
        recordAction(
                admin,
                ModerationActionType.CONTENT_APPROVED,
                targetType,
                targetId,
                null);
        integrationService.contentApprovalReviewed(
                response.submittedBy().userId(),
                admin.getId(),
                targetType,
                targetId,
                response.title(),
                true);
        return response;
    }

    @Transactional
    public PendingApprovalItemResponse reject(
            UUID adminUserId,
            ModerationTargetType targetType,
            UUID targetId,
            String reason) {
        User admin = requireApprover(adminUserId, targetType);
        String normalizedReason = normalizeReason(reason);
        PendingApprovalItemResponse response = switch (targetType) {
            case NOTE -> rejectNote(admin, targetId, normalizedReason);
            case MARKETPLACE_LISTING -> rejectListing(targetId, normalizedReason);
            case EVENT -> rejectEvent(targetId, normalizedReason);
            case DISCUSSION_QUESTION -> rejectQuestion(targetId, normalizedReason);
            case INTERNSHIP -> rejectInternship(targetId, normalizedReason);
            case LOST_FOUND_ITEM -> lostFoundService.rejectItem(
                    admin,
                    targetId,
                    normalizedReason);
            default -> throw unsupportedTarget();
        };
        recordAction(
                admin,
                ModerationActionType.CONTENT_REJECTED,
                targetType,
                targetId,
                normalizedReason);
        integrationService.contentApprovalReviewed(
                response.submittedBy().userId(),
                admin.getId(),
                targetType,
                targetId,
                response.title(),
                false);
        return response;
    }

    private Page<PendingApprovalItemResponse> combinedPendingPage(
            PageRequest pageRequest) {
        PageRequest fetchPage = PageRequest.of(0, COMBINED_FETCH_LIMIT);
        List<PendingApprovalItemResponse> combined =
                java.util.stream.Stream.of(
                                noteRepository.findAllByModerationStatusAndDeletedAtIsNull(
                                                NoteModerationStatus.PENDING,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(this::toPendingItem),
                                listingRepository.findAllByStatusAndDeletedAtIsNull(
                                                MarketplaceListingStatus.PENDING_REVIEW,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(this::toPendingItem),
                                eventRepository.findAllByStatusAndDeletedFalse(
                                                EventStatus.PENDING_REVIEW,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(this::toPendingItem),
                                questionRepository.findAllByStatusAndDeletedFalse(
                                                DiscussionQuestionStatus.PENDING_REVIEW,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(this::toPendingItem),
                                internshipRepository.findAllByStatusAndDeletedFalse(
                                                InternshipStatus.PENDING_REVIEW,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(this::toPendingItem),
                                lostFoundItemRepository.findAllByStatusAndDeletedAtIsNull(
                                                LostFoundItemStatus.PENDING_REVIEW,
                                                fetchPage)
                                        .getContent()
                                        .stream()
                                        .map(lostFoundService::toPendingApprovalItem))
                        .flatMap(stream -> stream)
                        .sorted(Comparator
                                .comparing(
                                        PendingApprovalItemResponse::submittedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()))
                                .thenComparing(PendingApprovalItemResponse::id))
                        .toList();
        return pageFromList(combined, pageRequest);
    }

    private Page<PendingApprovalItemResponse> pendingPageForTarget(
            ModerationTargetType targetType,
            PageRequest pageRequest) {
        return switch (targetType) {
            case NOTE -> noteRepository
                    .findAllByModerationStatusAndDeletedAtIsNull(
                            NoteModerationStatus.PENDING,
                            pageRequest)
                    .map(this::toPendingItem);
            case MARKETPLACE_LISTING -> listingRepository
                    .findAllByStatusAndDeletedAtIsNull(
                            MarketplaceListingStatus.PENDING_REVIEW,
                            pageRequest)
                    .map(this::toPendingItem);
            case EVENT -> eventRepository
                    .findAllByStatusAndDeletedFalse(
                            EventStatus.PENDING_REVIEW,
                            pageRequest)
                    .map(this::toPendingItem);
            case DISCUSSION_QUESTION -> questionRepository
                    .findAllByStatusAndDeletedFalse(
                            DiscussionQuestionStatus.PENDING_REVIEW,
                            pageRequest)
                    .map(this::toPendingItem);
            case INTERNSHIP -> internshipRepository
                    .findAllByStatusAndDeletedFalse(
                            InternshipStatus.PENDING_REVIEW,
                            pageRequest)
                    .map(this::toPendingItem);
            case LOST_FOUND_ITEM -> lostFoundItemRepository
                    .findAllByStatusAndDeletedAtIsNull(
                            LostFoundItemStatus.PENDING_REVIEW,
                            pageRequest)
                    .map(lostFoundService::toPendingApprovalItem);
            default -> throw unsupportedTarget();
        };
    }

    private Page<PendingApprovalItemResponse> pageFromList(
            List<PendingApprovalItemResponse> items,
            PageRequest pageRequest) {
        int start = (int) Math.min(pageRequest.getOffset(), items.size());
        int end = Math.min(start + pageRequest.getPageSize(), items.size());
        return new PageImpl<>(
                items.subList(start, end),
                pageRequest,
                items.size());
    }

    private PendingApprovalItemResponse approveNote(
            User admin,
            UUID noteId) {
        Note note = noteRepository.findActiveByIdForUpdate(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note"));
        note.approve(admin, clock.instant());
        return toPendingItem(note);
    }

    private PendingApprovalItemResponse rejectNote(
            User admin,
            UUID noteId,
            String reason) {
        Note note = noteRepository.findActiveByIdForUpdate(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note"));
        note.reject(admin, reason, clock.instant());
        return toPendingItem(note);
    }

    private PendingApprovalItemResponse approveListing(UUID listingId) {
        MarketplaceListing listing = listingRepository.findDetailedById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marketplace listing"));
        listing.approve();
        return toPendingItem(listing);
    }

    private PendingApprovalItemResponse rejectListing(
            UUID listingId,
            String reason) {
        MarketplaceListing listing = listingRepository.findDetailedById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marketplace listing"));
        listing.reject();
        return toPendingItem(listing);
    }

    private PendingApprovalItemResponse approveEvent(UUID eventId) {
        CampusEvent event = eventRepository.findActiveByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event"));
        event.approve();
        return toPendingItem(event);
    }

    private PendingApprovalItemResponse rejectEvent(
            UUID eventId,
            String reason) {
        CampusEvent event = eventRepository.findActiveByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event"));
        event.reject();
        return toPendingItem(event);
    }

    private PendingApprovalItemResponse approveQuestion(UUID questionId) {
        DiscussionQuestion question =
                questionRepository.findActiveByIdForUpdate(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Discussion question"));
        question.approve();
        return toPendingItem(question);
    }

    private PendingApprovalItemResponse rejectQuestion(
            UUID questionId,
            String reason) {
        DiscussionQuestion question =
                questionRepository.findActiveByIdForUpdate(questionId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Discussion question"));
        question.reject();
        return toPendingItem(question);
    }

    private PendingApprovalItemResponse approveInternship(UUID internshipId) {
        Internship internship =
                internshipRepository.findActiveByIdForUpdate(internshipId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Internship"));
        internship.approve();
        return toPendingItem(internship);
    }

    private PendingApprovalItemResponse rejectInternship(
            UUID internshipId,
            String reason) {
        Internship internship =
                internshipRepository.findActiveByIdForUpdate(internshipId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Internship"));
        internship.reject();
        return toPendingItem(internship);
    }

    private PendingApprovalItemResponse toPendingItem(Note note) {
        return new PendingApprovalItemResponse(
                note.getId(),
                ModerationTargetType.NOTE,
                note.getTitle(),
                note.getDescription(),
                toReporter(note.getUploader()),
                note.getCreatedAt(),
                note.getModerationStatus().name(),
                null,
                "/notes/" + note.getId());
    }

    private PendingApprovalItemResponse toPendingItem(
            MarketplaceListing listing) {
        String previewUrl = listing.getImages().isEmpty()
                ? null
                : listing.getImages().get(0).getImageUrl();
        return new PendingApprovalItemResponse(
                listing.getId(),
                ModerationTargetType.MARKETPLACE_LISTING,
                listing.getTitle(),
                listing.getDescription(),
                toReporter(listing.getSeller()),
                listing.getCreatedAt(),
                listing.getStatus().name(),
                previewUrl,
                "/marketplace/" + listing.getId());
    }

    private PendingApprovalItemResponse toPendingItem(CampusEvent event) {
        return new PendingApprovalItemResponse(
                event.getId(),
                ModerationTargetType.EVENT,
                event.getTitle(),
                event.getDescription(),
                toReporter(event.getOrganizer()),
                event.getCreatedAt(),
                event.getStatus().name(),
                null,
                "/events/" + event.getId());
    }

    private PendingApprovalItemResponse toPendingItem(
            DiscussionQuestion question) {
        return new PendingApprovalItemResponse(
                question.getId(),
                ModerationTargetType.DISCUSSION_QUESTION,
                question.getTitle(),
                question.getBody(),
                toReporter(question.getAuthor()),
                question.getCreatedAt(),
                question.getStatus().name(),
                null,
                "/discussions/questions/" + question.getId());
    }

    private PendingApprovalItemResponse toPendingItem(Internship internship) {
        return new PendingApprovalItemResponse(
                internship.getId(),
                ModerationTargetType.INTERNSHIP,
                internship.getTitle(),
                internship.getDescription(),
                toReporter(internship.getPoster()),
                internship.getCreatedAt(),
                internship.getStatus().name(),
                null,
                "/internships/" + internship.getId());
    }

    private ReporterSummaryResponse toReporter(User user) {
        return new ReporterSummaryResponse(
                user.getId(),
                safeFullName(user));
    }

    private String safeFullName(User user) {
        StudentProfile profile = user.getStudentProfile();
        if (profile == null
                || profile.getVisibility() != ProfileVisibility.PUBLIC) {
            return null;
        }
        return profile.getFullName();
    }

    private User requireAdmin(UUID adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        adminAuthorizationService.requireAdmin(admin.getId(), admin.getEmail());
        return admin;
    }

    private User requireApprover(
            UUID userId,
            ModerationTargetType targetType) {
        if (targetType == ModerationTargetType.LOST_FOUND_ITEM) {
            moderatorAuthorizationService.requireActiveModerator(userId);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
        }
        return requireAdmin(userId);
    }

    private void recordAction(
            User admin,
            ModerationActionType actionType,
            ModerationTargetType targetType,
            UUID targetId,
            String reason) {
        actionRepository.save(new ModerationAction(
                admin,
                null,
                actionType,
                targetType,
                targetId,
                reason,
                null));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ModerationConflictException(
                    "REJECTION_REASON_REQUIRED",
                    "A rejection reason is required.");
        }
        return reason.trim();
    }

    private ModerationConflictException unsupportedTarget() {
        return new ModerationConflictException(
                "UNSUPPORTED_APPROVAL_TARGET",
                "This content type does not support approval actions.");
    }
}
