package com.campusone.moderation.service;

import com.campusone.moderation.dto.response.ModeratorStatusResponse;
import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.exception.ModeratorAccessDeniedException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ModeratorRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModeratorAuthorizationService {

    private final ModeratorRepository moderatorRepository;
    private final ModerationMapper moderationMapper;

    public ModeratorAuthorizationService(
            ModeratorRepository moderatorRepository,
            ModerationMapper moderationMapper) {
        this.moderatorRepository = moderatorRepository;
        this.moderationMapper = moderationMapper;
    }

    @Transactional(readOnly = true)
    public ModeratorStatusResponse getStatus(UUID userId) {
        return moderationMapper.toStatus(
                moderatorRepository.findDetailedByUserId(userId)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public Moderator requireActiveModerator(UUID userId) {
        Moderator moderator =
                moderatorRepository.findDetailedByUserId(userId)
                        .orElseThrow(
                                ModeratorAccessDeniedException::new);
        if (!moderator.isActive()
                || !isAuthorizedRole(moderator.getRole())) {
            throw new ModeratorAccessDeniedException();
        }
        return moderator;
    }

    private boolean isAuthorizedRole(ModeratorRole role) {
        return role == ModeratorRole.ADMIN
                || role == ModeratorRole.MODERATOR;
    }
}
