package com.campusone.moderation.service;

import com.campusone.moderation.dto.response.ModeratorStatusResponse;
import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.exception.ModeratorAccessDeniedException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModeratorAuthorizationService {

    private final ModeratorRepository moderatorRepository;
    private final ModerationMapper moderationMapper;
    private final UserRepository userRepository;
    private final NoteAdminAuthorizationService adminAuthorizationService;

    public ModeratorAuthorizationService(
            ModeratorRepository moderatorRepository,
            ModerationMapper moderationMapper,
            UserRepository userRepository,
            NoteAdminAuthorizationService adminAuthorizationService) {
        this.moderatorRepository = moderatorRepository;
        this.moderationMapper = moderationMapper;
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public ModeratorStatusResponse getStatus(UUID userId) {
        Moderator moderator = moderatorRepository.findDetailedByUserId(userId)
                .orElse(null);
        ModeratorStatusResponse status = moderationMapper.toStatus(moderator);
        if (status.activeModerator()) {
            return status;
        }
        return userRepository.findById(userId)
                .filter(user -> adminAuthorizationService.canManage(
                        user.getId(),
                        user.getEmail()))
                .map(user -> new ModeratorStatusResponse(
                        true,
                        ModeratorRole.ADMIN,
                        null,
                        null))
                .orElse(status);
    }

    @Transactional(readOnly = true)
    public Moderator requireActiveModerator(UUID userId) {
        Moderator moderator = moderatorRepository.findDetailedByUserId(userId)
                .orElse(null);
        if (moderator != null
                && moderator.isActive()
                && isAuthorizedRole(moderator.getRole())) {
            return moderator;
        }

        return userRepository.findById(userId)
                .filter(user -> adminAuthorizationService.canManage(
                        user.getId(),
                        user.getEmail()))
                .map(user -> new Moderator(
                        user,
                        ModeratorRole.ADMIN,
                        null))
                .orElseThrow(ModeratorAccessDeniedException::new);
    }

    private boolean isAuthorizedRole(ModeratorRole role) {
        return role == ModeratorRole.ADMIN
                || role == ModeratorRole.MODERATOR;
    }
}
