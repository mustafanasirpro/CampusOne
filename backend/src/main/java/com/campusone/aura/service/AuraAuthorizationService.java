package com.campusone.aura.service;

import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuraAuthorizationService {

    private final UserRepository userRepository;
    private final NoteAdminAuthorizationService adminAuthorizationService;

    public AuraAuthorizationService(
            UserRepository userRepository,
            NoteAdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public void requireAdmin(UUID userId) {
        String email = userRepository.findById(userId)
                .map(user -> user.getEmail())
                .orElse("");
        if (!adminAuthorizationService.canManage(userId, email)) {
            throw new AccessDeniedException(
                    "Only admins can manage AURA timetable data.");
        }
    }

    @Transactional(readOnly = true)
    public UUID requireAdminUniversity(UUID userId) {
        var user = userRepository.findDetailedById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Only admins can manage AURA timetable data."));
        if (!adminAuthorizationService.canManage(userId, user.getEmail())) {
            throw new AccessDeniedException(
                    "Only admins can manage AURA timetable data.");
        }
        if (user.getStudentProfile() == null
                || user.getStudentProfile().getUniversity() == null) {
            throw new AccessDeniedException(
                    "A university profile is required to manage AURA.");
        }
        return user.getStudentProfile().getUniversity().getId();
    }

    @Transactional(readOnly = true)
    public UUID requireUniversity(UUID userId) {
        var user = requireUniversityProfile(
                userId,
                "A university profile is required to use AURA.");
        return user.getStudentProfile().getUniversity().getId();
    }

    @Transactional(readOnly = true)
    public boolean canManage(UUID userId) {
        var user = requireUniversityProfile(
                userId,
                "A university profile is required to use AURA.");
        return adminAuthorizationService.canManage(userId, user.getEmail());
    }

    private com.campusone.user.entity.User requireUniversityProfile(
            UUID userId,
            String missingProfileMessage) {
        var user = userRepository.findDetailedById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                    missingProfileMessage));
        if (user.getStudentProfile() == null
                || user.getStudentProfile().getUniversity() == null) {
            throw new AccessDeniedException(
                    missingProfileMessage);
        }
        return user;
    }
}
