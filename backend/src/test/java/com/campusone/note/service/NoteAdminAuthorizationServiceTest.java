package com.campusone.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.NoteManagementAccessDeniedException;
import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.repository.ModeratorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteAdminAuthorizationServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private Moderator moderator;

    @Test
    void canManage_activeAdminAssignment_returnsTrue() {
        when(moderatorRepository.findById(USER_ID))
                .thenReturn(Optional.of(moderator));
        when(moderator.isActive()).thenReturn(true);
        when(moderator.getRole()).thenReturn(ModeratorRole.ADMIN);
        NoteAdminAuthorizationService service =
                new NoteAdminAuthorizationService(
                        moderatorRepository,
                        "");

        assertThat(service.canManage(USER_ID, "admin@example.com"))
                .isTrue();
    }

    @Test
    void canManage_activeModeratorAssignment_returnsFalse() {
        when(moderatorRepository.findById(USER_ID))
                .thenReturn(Optional.of(moderator));
        when(moderator.isActive()).thenReturn(true);
        when(moderator.getRole()).thenReturn(ModeratorRole.MODERATOR);
        NoteAdminAuthorizationService service =
                new NoteAdminAuthorizationService(
                        moderatorRepository,
                        "");

        assertThat(service.canManage(USER_ID, "moderator@example.com"))
                .isFalse();
    }

    @Test
    void canManage_configuredFallbackEmail_returnsTrue() {
        when(moderatorRepository.findById(USER_ID))
                .thenReturn(Optional.empty());
        NoteAdminAuthorizationService service =
                new NoteAdminAuthorizationService(
                        moderatorRepository,
                        "first@example.com, Admin@Example.com ");

        assertThat(service.canManage(USER_ID, "admin@example.com"))
                .isTrue();
    }

    @Test
    void requireAdmin_normalUser_returnsCleanForbiddenError() {
        when(moderatorRepository.findById(USER_ID))
                .thenReturn(Optional.empty());
        NoteAdminAuthorizationService service =
                new NoteAdminAuthorizationService(
                        moderatorRepository,
                        "");

        assertThatThrownBy(() -> service.requireAdmin(
                USER_ID,
                "student@example.com"))
                .isInstanceOf(NoteManagementAccessDeniedException.class)
                .hasMessage("Only admins can upload or manage notes.");
    }
}
