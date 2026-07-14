package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuraAuthorizationServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000001");
    private static final UUID STUDENT_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000002");

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    @Test
    void requireAdmin_allowsConfiguredCampusOneAdmin() {
        User admin = user(ADMIN_ID, "admin@example.com");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(adminAuthorizationService.canManage(
                ADMIN_ID,
                "admin@example.com")).thenReturn(true);

        new AuraAuthorizationService(userRepository, adminAuthorizationService)
                .requireAdmin(ADMIN_ID);
    }

    @Test
    void requireAdmin_rejectsNormalStudent() {
        User student = user(STUDENT_ID, "student@example.com");
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        AuraAuthorizationService service = new AuraAuthorizationService(
                userRepository,
                adminAuthorizationService);

        assertThatThrownBy(() -> service.requireAdmin(STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only admins can manage AURA timetable data.");
    }

    private User user(UUID id, String email) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
