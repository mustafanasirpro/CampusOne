package com.campusone.note.service;

import com.campusone.common.exception.NoteManagementAccessDeniedException;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.repository.ModeratorRepository;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteAdminAuthorizationService {

    private final ModeratorRepository moderatorRepository;
    private final Set<String> fallbackAdminEmails;

    public NoteAdminAuthorizationService(
            ModeratorRepository moderatorRepository,
            @Value("${app.notes.admin-upload-emails:}")
            String fallbackAdminEmails) {
        this.moderatorRepository = moderatorRepository;
        this.fallbackAdminEmails = parseEmails(fallbackAdminEmails);
    }

    @Transactional(readOnly = true)
    public boolean canManage(UUID userId, String email) {
        boolean activeAdmin = moderatorRepository.findById(userId)
                .filter(moderator -> moderator.isActive()
                        && moderator.getRole() == ModeratorRole.ADMIN)
                .isPresent();
        return activeAdmin || fallbackAdminEmails.contains(normalize(email));
    }

    @Transactional(readOnly = true)
    public void requireAdmin(UUID userId, String email) {
        if (!canManage(userId, email)) {
            throw new NoteManagementAccessDeniedException();
        }
    }

    private Set<String> parseEmails(String configuredEmails) {
        if (configuredEmails == null || configuredEmails.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(configuredEmails.split(","))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
