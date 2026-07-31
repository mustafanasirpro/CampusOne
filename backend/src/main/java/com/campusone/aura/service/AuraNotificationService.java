package com.campusone.aura.service;

import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import com.campusone.user.repository.UserRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraNotificationService {

    private static final Logger LOG =
            LoggerFactory.getLogger(AuraNotificationService.class);

    private final NotificationService notificationService;
    private final ModeratorRepository moderatorRepository;
    private final UserRepository userRepository;
    private final Set<String> fallbackAdminEmails;

    public AuraNotificationService(
            NotificationService notificationService,
            ModeratorRepository moderatorRepository,
            UserRepository userRepository,
            @Value("${app.notes.admin-upload-emails:}") String fallbackAdminEmails) {
        this.notificationService = notificationService;
        this.moderatorRepository = moderatorRepository;
        this.userRepository = userRepository;
        this.fallbackAdminEmails = parseEmails(fallbackAdminEmails);
    }

    public void notifyUniversityAdmins(
            UUID universityId,
            UUID excludedUserId,
            String title,
            String message,
            UUID targetId) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        moderatorRepository.findActiveAdminUserIds().forEach(userId ->
                userRepository.findDetailedById(userId)
                        .filter(user -> hasUniversity(user, universityId))
                        .ifPresent(user -> recipients.add(userId)));
        fallbackAdminEmails.forEach(email ->
                userRepository.findByEmailIgnoreCase(email)
                        .flatMap(user -> userRepository.findDetailedById(user.getId()))
                        .filter(user -> hasUniversity(user, universityId))
                        .ifPresent(user -> recipients.add(user.getId())));
        recipients.remove(excludedUserId);
        safelyDispatch(targetId, () ->
                notificationService.createBulkNotifications(
                        recipients,
                        NotificationType.AURA_UPDATE,
                        title,
                        message,
                        NotificationTargetType.AURA_RESOLUTION,
                        targetId,
                        "/admin/aura"));
    }

    public void notifyStudent(
            UUID studentUserId,
            String title,
            String message,
            UUID targetId) {
        safelyDispatch(targetId, () ->
                notificationService.createNotification(
                        studentUserId,
                        NotificationType.AURA_UPDATE,
                        title,
                        message,
                        NotificationTargetType.AURA_RESOLUTION,
                        targetId,
                        "/timetable"));
    }

    public void notifyTimetablePublished(
            Iterable<UUID> studentUserIds,
            UUID actorUserId,
            UUID versionId) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        studentUserIds.forEach(recipients::add);
        recipients.remove(actorUserId);
        safelyDispatch(versionId, () ->
                notificationService.createBulkNotifications(
                        recipients,
                        NotificationType.AURA_UPDATE,
                        "A new timetable is available",
                        "Your latest university timetable is ready to view.",
                        NotificationTargetType.AURA_TIMETABLE,
                        versionId,
                        "/timetable"));
    }

    public void notifyInstructorsTimetablePublished(
            Iterable<UUID> instructorUserIds,
            UUID actorUserId,
            UUID versionId) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        instructorUserIds.forEach(recipients::add);
        recipients.remove(actorUserId);
        safelyDispatch(versionId, () ->
                notificationService.createBulkNotifications(
                        recipients,
                        NotificationType.AURA_UPDATE,
                        "Your teaching timetable is available",
                        "The latest published teaching timetable is ready to view.",
                        NotificationTargetType.AURA_TIMETABLE,
                        versionId,
                        "/instructor-timetable"));
    }

    private void safelyDispatch(UUID targetId, Runnable dispatch) {
        try {
            dispatch.run();
        } catch (RuntimeException exception) {
            LOG.warn(
                    "AURA notification delivery failed safely for target {}: {}",
                    targetId,
                    exception.getClass().getSimpleName());
        }
    }

    private boolean hasUniversity(
            com.campusone.user.entity.User user,
            UUID universityId) {
        return user.getStudentProfile() != null
                && user.getStudentProfile().getUniversity() != null
                && universityId.equals(
                        user.getStudentProfile().getUniversity().getId());
    }

    private Set<String> parseEmails(String configuredEmails) {
        if (configuredEmails == null || configuredEmails.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredEmails.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
