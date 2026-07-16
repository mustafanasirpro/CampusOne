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

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraNotificationService {

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
        notificationService.createBulkNotifications(
                recipients,
                NotificationType.AURA_UPDATE,
                title,
                message,
                NotificationTargetType.AURA_RESOLUTION,
                targetId,
                "/admin/aura");
    }

    public void notifyStudent(
            UUID studentUserId,
            String title,
            String message,
            UUID targetId) {
        notificationService.createNotification(
                studentUserId,
                NotificationType.AURA_UPDATE,
                title,
                message,
                NotificationTargetType.AURA_RESOLUTION,
                targetId,
                "/timetable");
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
