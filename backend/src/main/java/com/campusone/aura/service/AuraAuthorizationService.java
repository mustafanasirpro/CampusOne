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
}
