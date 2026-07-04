package com.campusone.common.service;

import com.campusone.gamification.entity.GamificationActionType;
import com.campusone.gamification.entity.GamificationSourceType;
import com.campusone.gamification.service.GamificationService;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityIntegrationService {

    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    public CommunityIntegrationService(
            GamificationService gamificationService,
            NotificationService notificationService) {
        this.gamificationService = gamificationService;
        this.notificationService = notificationService;
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
}
