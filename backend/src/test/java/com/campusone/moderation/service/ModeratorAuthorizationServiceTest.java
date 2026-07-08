package com.campusone.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campusone.moderation.entity.Moderator;
import com.campusone.moderation.entity.ModeratorRole;
import com.campusone.moderation.exception.ModeratorAccessDeniedException;
import com.campusone.moderation.mapper.ModerationMapper;
import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ModeratorAuthorizationServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    private ModeratorAuthorizationService service;
    private Moderator moderator;

    @BeforeEach
    void setUp() {
        User user = new User(
                "moderator@example.com",
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        moderator = new Moderator(
                user,
                ModeratorRole.MODERATOR,
                null);
        ReflectionTestUtils.setField(moderator, "userId", USER_ID);
        ReflectionTestUtils.setField(moderator, "assignedAt", NOW);
        ReflectionTestUtils.setField(moderator, "createdAt", NOW);
        ReflectionTestUtils.setField(moderator, "updatedAt", NOW);
        service = new ModeratorAuthorizationService(
                moderatorRepository,
                new ModerationMapper(),
                userRepository,
                adminAuthorizationService);
    }

    @Test
    void getStatus_normalUser_returnsInactive() {
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        var response = service.getStatus(USER_ID);

        assertThat(response.activeModerator()).isFalse();
        assertThat(response.role()).isNull();
    }

    @Test
    void getStatus_activeModerator_returnsRole() {
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.of(moderator));

        var response = service.getStatus(USER_ID);

        assertThat(response.activeModerator()).isTrue();
        assertThat(response.role())
                .isEqualTo(ModeratorRole.MODERATOR);
    }

    @Test
    void getStatus_fallbackAdminEmail_returnsAdminAccess() {
        User user = user("fallback-admin@example.com");
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(adminAuthorizationService.canManage(
                USER_ID,
                "fallback-admin@example.com"))
                .thenReturn(true);

        var response = service.getStatus(USER_ID);

        assertThat(response.activeModerator()).isTrue();
        assertThat(response.role()).isEqualTo(ModeratorRole.ADMIN);
    }

    @Test
    void requireActiveModerator_normalUser_rejectsAccess() {
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.requireActiveModerator(USER_ID))
                .isInstanceOf(
                        ModeratorAccessDeniedException.class);
    }

    @Test
    void requireActiveModerator_inactiveAssignment_rejectsAccess() {
        ReflectionTestUtils.setField(moderator, "active", false);
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.of(moderator));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.requireActiveModerator(USER_ID))
                .isInstanceOf(
                        ModeratorAccessDeniedException.class);
    }

    @Test
    void requireActiveModerator_fallbackAdminEmail_returnsAdminModerator() {
        User user = user("fallback-admin@example.com");
        when(moderatorRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(adminAuthorizationService.canManage(
                USER_ID,
                "fallback-admin@example.com"))
                .thenReturn(true);

        Moderator fallbackModerator = service.requireActiveModerator(USER_ID);

        assertThat(fallbackModerator.getUser()).isSameAs(user);
        assertThat(fallbackModerator.getRole())
                .isEqualTo(ModeratorRole.ADMIN);
        assertThat(fallbackModerator.isActive()).isTrue();
    }

    private User user(String email) {
        User user = new User(
                email,
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
