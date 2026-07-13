package com.campusone.common.service;

import com.campusone.gamification.entity.GamificationActionType;
import com.campusone.gamification.entity.GamificationSourceType;
import com.campusone.gamification.service.GamificationService;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import com.campusone.user.repository.UserRepository;
import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityIntegrationService {

    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final ModeratorRepository moderatorRepository;
    private final UserRepository userRepository;
    private final Set<String> fallbackAdminEmails;

    public CommunityIntegrationService(
            GamificationService gamificationService,
            NotificationService notificationService,
            ModeratorRepository moderatorRepository,
            UserRepository userRepository,
            @Value("${app.notes.admin-upload-emails:}")
            String fallbackAdminEmails) {
        this.gamificationService = gamificationService;
        this.notificationService = notificationService;
        this.moderatorRepository = moderatorRepository;
        this.userRepository = userRepository;
        this.fallbackAdminEmails = parseEmails(fallbackAdminEmails);
    }

    @Transactional
    public void noteCreated(UUID creatorUserId, UUID noteId) {
        awardXp(
                creatorUserId,
                GamificationActionType.NOTE_CREATED,
                15,
                GamificationSourceType.NOTE,
                noteId,
                "Created a study note");
    }

    @Transactional
    public void noteSubmittedForApproval(
            UUID submitterUserId,
            UUID noteId,
            String noteTitle) {
        notifyApprovalRecipients(
                submitterUserId,
                "New note pending approval",
                "A student submitted \"%s\" for admin review."
                        .formatted(safeTitle(noteTitle, "a note")),
                NotificationTargetType.NOTE,
                noteId);
    }

    @Transactional
    public void noteDownloaded(
            UUID ownerUserId,
            UUID downloaderUserId,
            UUID noteId) {
        if (ownerUserId.equals(downloaderUserId)) {
            return;
        }
        awardXp(
                ownerUserId,
                GamificationActionType.NOTE_DOWNLOADED,
                3,
                GamificationSourceType.NOTE,
                noteId,
                "Another student downloaded your note");
    }

    @Transactional
    public void noteRated(
            UUID ownerUserId,
            UUID raterUserId,
            UUID noteId) {
        if (ownerUserId.equals(raterUserId)) {
            return;
        }
        awardXp(
                ownerUserId,
                GamificationActionType.NOTE_RATED,
                2,
                GamificationSourceType.NOTE,
                noteId,
                "Another student rated your note");
    }

    @Transactional
    public void marketplaceListingCreated(
            UUID creatorUserId,
            UUID listingId) {
        awardXp(
                creatorUserId,
                GamificationActionType.MARKETPLACE_LISTING_CREATED,
                10,
                GamificationSourceType.MARKETPLACE_LISTING,
                listingId,
                "Created a marketplace listing");
    }

    @Transactional
    public void marketplaceListingSubmittedForApproval(
            UUID submitterUserId,
            UUID listingId,
            String listingTitle) {
        notifyApprovalRecipients(
                submitterUserId,
                "New marketplace listing pending approval",
                "A student submitted \"%s\" for admin review."
                        .formatted(safeTitle(listingTitle, "a marketplace listing")),
                NotificationTargetType.MARKETPLACE_LISTING,
                listingId);
    }

    @Transactional
    public void discussionQuestionCreated(
            UUID authorUserId,
            UUID questionId) {
        awardXp(
                authorUserId,
                GamificationActionType.DISCUSSION_QUESTION_CREATED,
                10,
                GamificationSourceType.DISCUSSION_QUESTION,
                questionId,
                "Asked a discussion question");
    }

    @Transactional
    public void discussionQuestionSubmittedForApproval(
            UUID submitterUserId,
            UUID questionId,
            String questionTitle) {
        notifyApprovalRecipients(
                submitterUserId,
                "New discussion pending approval",
                "A student submitted \"%s\" for admin review."
                        .formatted(safeTitle(questionTitle, "a discussion")),
                NotificationTargetType.DISCUSSION_QUESTION,
                questionId);
    }

    @Transactional
    public void discussionAnswerCreated(
            UUID answerAuthorUserId,
            UUID questionAuthorUserId,
            UUID questionId,
            UUID answerId) {
        awardXp(
                answerAuthorUserId,
                GamificationActionType.DISCUSSION_ANSWER_CREATED,
                12,
                GamificationSourceType.DISCUSSION_ANSWER,
                answerId,
                "Answered a discussion question");
        if (!answerAuthorUserId.equals(questionAuthorUserId)) {
            notificationService.createNotification(
                    questionAuthorUserId,
                    NotificationType.DISCUSSION_REPLY,
                    "New answer on your question",
                    "A student answered your discussion question.",
                    NotificationTargetType.DISCUSSION_QUESTION,
                    questionId,
                    "/discussions/questions/" + questionId);
        }
    }

    @Transactional
    public void discussionAnswerAccepted(
            UUID answerAuthorUserId,
            UUID questionAuthorUserId,
            UUID questionId,
            UUID answerId) {
        awardXp(
                answerAuthorUserId,
                GamificationActionType.DISCUSSION_ANSWER_ACCEPTED,
                25,
                GamificationSourceType.DISCUSSION_ANSWER,
                answerId,
                "Your answer was accepted");
        if (!answerAuthorUserId.equals(questionAuthorUserId)) {
            notificationService.createNotification(
                    answerAuthorUserId,
                    NotificationType.DISCUSSION_ACCEPTED,
                    "Your answer was accepted",
                    "Your answer was accepted as the best answer.",
                    NotificationTargetType.DISCUSSION_ANSWER,
                    answerId,
                    "/discussions/questions/" + questionId);
        }
    }

    @Transactional
    public void eventCreated(UUID organizerUserId, UUID eventId) {
        awardXp(
                organizerUserId,
                GamificationActionType.EVENT_CREATED,
                10,
                GamificationSourceType.EVENT,
                eventId,
                "Created a campus event");
    }

    @Transactional
    public void eventSubmittedForApproval(
            UUID submitterUserId,
            UUID eventId,
            String eventTitle) {
        notifyApprovalRecipients(
                submitterUserId,
                "New event pending approval",
                "A student submitted \"%s\" for admin review."
                        .formatted(safeTitle(eventTitle, "an event")),
                NotificationTargetType.EVENT,
                eventId);
    }

    @Transactional
    public void eventJoined(
            UUID joiningUserId,
            UUID organizerUserId,
            UUID eventId) {
        awardXp(
                joiningUserId,
                GamificationActionType.EVENT_JOINED,
                5,
                GamificationSourceType.EVENT,
                eventId,
                "Joined a campus event");
        if (!joiningUserId.equals(organizerUserId)) {
            notificationService.createNotification(
                    organizerUserId,
                    NotificationType.EVENT_UPDATE,
                    "New participant joined your event",
                    "A student joined your campus event.",
                    NotificationTargetType.EVENT,
                    eventId,
                    "/events/" + eventId);
        }
    }

    @Transactional
    public void eventUpdated(
            Collection<UUID> participantUserIds,
            UUID eventId,
            boolean cancelled) {
        String title = cancelled ? "Event cancelled" : "Event updated";
        String message = cancelled
                ? "An event you joined was cancelled."
                : "An event you joined was updated.";
        notificationService.createBulkNotifications(
                participantUserIds,
                NotificationType.EVENT_UPDATE,
                title,
                message,
                NotificationTargetType.EVENT,
                eventId,
                "/events/" + eventId);
    }

    @Transactional
    public void internshipCreated(UUID posterUserId, UUID internshipId) {
        awardXp(
                posterUserId,
                GamificationActionType.INTERNSHIP_CREATED,
                15,
                GamificationSourceType.INTERNSHIP,
                internshipId,
                "Shared an internship opportunity");
    }

    @Transactional
    public void internshipSubmittedForApproval(
            UUID submitterUserId,
            UUID internshipId,
            String internshipTitle) {
        notifyApprovalRecipients(
                submitterUserId,
                "New internship pending approval",
                "A student submitted \"%s\" for admin review."
                        .formatted(safeTitle(internshipTitle, "an internship")),
                NotificationTargetType.INTERNSHIP,
                internshipId);
    }

    @Transactional
    public void lostFoundItemSubmittedForApproval(
            UUID submitterUserId,
            UUID itemId,
            String itemTitle) {
        LinkedHashSet<UUID> recipients = approvalRecipientUserIds();
        recipients.remove(submitterUserId);
        notificationService.createBulkNotifications(
                recipients,
                NotificationType.LOST_FOUND_UPDATE,
                "New Lost & Found item pending approval",
                "A student submitted \"%s\" for Lost & Found review."
                        .formatted(safeTitle(itemTitle, "a Lost & Found item")),
                NotificationTargetType.LOST_FOUND_ITEM,
                itemId,
                "/admin");
    }

    @Transactional
    public void lostFoundClaimCreated(
            UUID reporterUserId,
            UUID claimantUserId,
            UUID itemId,
            UUID claimId,
            String itemTitle) {
        if (reporterUserId.equals(claimantUserId)) {
            return;
        }
        notificationService.createNotification(
                reporterUserId,
                NotificationType.LOST_FOUND_UPDATE,
                "New claim on your Lost & Found item",
                "A student submitted a claim for \"%s\"."
                        .formatted(safeTitle(itemTitle, "your item")),
                NotificationTargetType.LOST_FOUND_CLAIM,
                claimId,
                "/lost-found/" + itemId);
    }

    @Transactional
    public void lostFoundClaimReviewed(
            UUID claimantUserId,
            UUID reviewerUserId,
            UUID itemId,
            UUID claimId,
            String itemTitle,
            boolean approved) {
        if (claimantUserId.equals(reviewerUserId)) {
            return;
        }
        notificationService.createNotification(
                claimantUserId,
                NotificationType.LOST_FOUND_UPDATE,
                approved ? "Your claim was approved" : "Your claim was declined",
                approved
                        ? "Your claim for \"%s\" was approved. Arrange handover safely."
                                .formatted(safeTitle(itemTitle, "the item"))
                        : "Your claim for \"%s\" was reviewed and declined."
                                .formatted(safeTitle(itemTitle, "the item")),
                NotificationTargetType.LOST_FOUND_CLAIM,
                claimId,
                "/lost-found/" + itemId);
    }

    @Transactional
    public void lostFoundClaimCompleted(
            UUID claimantUserId,
            UUID reporterUserId,
            UUID itemId,
            UUID claimId,
            String itemTitle) {
        notificationService.createBulkNotifications(
                java.util.List.of(claimantUserId, reporterUserId),
                NotificationType.LOST_FOUND_UPDATE,
                "Lost & Found handover completed",
                "\"%s\" has been marked as resolved."
                        .formatted(safeTitle(itemTitle, "The item")),
                NotificationTargetType.LOST_FOUND_CLAIM,
                claimId,
                "/lost-found/" + itemId);
    }

    @Transactional
    public void contentApprovalReviewed(
            UUID submitterUserId,
            UUID moderatorUserId,
            ModerationTargetType targetType,
            UUID targetId,
            String title,
            boolean approved) {
        NotificationTargetType notificationTargetType =
                toNotificationTargetType(targetType);
        notificationService.markUnreadTargetNotificationsRead(
                NotificationType.ADMIN_MESSAGE,
                notificationTargetType,
                targetId);
        if (submitterUserId == null || submitterUserId.equals(moderatorUserId)) {
            return;
        }
        String safeTitle = safeTitle(title, "your submission");
        notificationService.createNotification(
                submitterUserId,
                NotificationType.ADMIN_MESSAGE,
                approved
                        ? "Your submission was approved"
                        : "Your submission was rejected",
                approved
                        ? "\"%s\" is now visible on CampusOne."
                                .formatted(safeTitle)
                        : "\"%s\" was reviewed and will remain hidden."
                                .formatted(safeTitle),
                notificationTargetType,
                targetId,
                detailUrl(targetType, targetId));
    }

    @Transactional
    public void moderationReportCreated(
            UUID reporterUserId,
            Collection<UUID> activeModeratorUserIds,
            UUID reportId) {
        LinkedHashSet<UUID> recipients =
                new LinkedHashSet<>(activeModeratorUserIds);
        recipients.remove(reporterUserId);
        notificationService.createBulkNotifications(
                recipients,
                NotificationType.ADMIN_MESSAGE,
                "New content report",
                "A new content report needs review.",
                NotificationTargetType.SYSTEM,
                reportId,
                "/admin/moderation/reports/" + reportId);
    }

    @Transactional
    public void moderationReportReviewed(
            UUID reporterUserId,
            UUID moderatorUserId,
            UUID reportId,
            boolean resolved) {
        if (reporterUserId.equals(moderatorUserId)) {
            return;
        }
        notificationService.createNotification(
                reporterUserId,
                NotificationType.ADMIN_MESSAGE,
                resolved
                        ? "Your report was resolved"
                        : "Your report was dismissed",
                "Your content report has been reviewed.",
                NotificationTargetType.SYSTEM,
                reportId,
                "/moderation/reports/my/" + reportId);
    }

    private void awardXp(
            UUID userId,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description) {
        gamificationService.awardXpIfNotAlreadyAwarded(
                userId,
                actionType,
                points,
                sourceType,
                sourceId,
                description);
    }

    private void notifyApprovalRecipients(
            UUID submitterUserId,
            String title,
            String message,
            NotificationTargetType targetType,
            UUID targetId) {
        LinkedHashSet<UUID> recipients = approvalRecipientUserIds();
        recipients.remove(submitterUserId);
        notificationService.createBulkNotifications(
                recipients,
                NotificationType.ADMIN_MESSAGE,
                title,
                message,
                targetType,
                targetId,
                "/admin");
    }

    private LinkedHashSet<UUID> approvalRecipientUserIds() {
        LinkedHashSet<UUID> recipients =
                new LinkedHashSet<>(moderatorRepository.findActiveModeratorUserIds());
        fallbackAdminEmails.forEach(email ->
                userRepository.findByEmailIgnoreCase(email)
                        .ifPresent(user -> recipients.add(user.getId())));
        return recipients;
    }

    private Set<String> parseEmails(String configuredEmails) {
        if (configuredEmails == null || configuredEmails.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredEmails.split(","))
                .map(this::normalizeEmail)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeEmail(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private String safeTitle(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value.trim();
    }

    private NotificationTargetType toNotificationTargetType(
            ModerationTargetType targetType) {
        return switch (targetType) {
            case NOTE -> NotificationTargetType.NOTE;
            case MARKETPLACE_LISTING -> NotificationTargetType.MARKETPLACE_LISTING;
            case EVENT -> NotificationTargetType.EVENT;
            case DISCUSSION_QUESTION -> NotificationTargetType.DISCUSSION_QUESTION;
            case DISCUSSION_ANSWER -> NotificationTargetType.DISCUSSION_ANSWER;
            case INTERNSHIP -> NotificationTargetType.INTERNSHIP;
            case LOST_FOUND_ITEM -> NotificationTargetType.LOST_FOUND_ITEM;
            case USER_PROFILE -> NotificationTargetType.USER;
            case AI_GENERATED_ITEM, SYSTEM -> NotificationTargetType.SYSTEM;
        };
    }

    private String detailUrl(
            ModerationTargetType targetType,
            UUID targetId) {
        return switch (targetType) {
            case NOTE -> "/notes/" + targetId;
            case MARKETPLACE_LISTING -> "/marketplace/" + targetId;
            case EVENT -> "/events/" + targetId;
            case DISCUSSION_QUESTION -> "/discussions/questions/" + targetId;
            case INTERNSHIP -> "/internships/" + targetId;
            case LOST_FOUND_ITEM -> "/lost-found/" + targetId;
            case DISCUSSION_ANSWER, AI_GENERATED_ITEM, USER_PROFILE, SYSTEM ->
                    "/admin";
        };
    }
}
