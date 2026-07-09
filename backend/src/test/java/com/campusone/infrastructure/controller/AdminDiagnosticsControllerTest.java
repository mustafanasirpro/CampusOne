package com.campusone.infrastructure.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.campusone.auth.service.PasswordResetMailer;
import com.campusone.common.exception.NoteManagementAccessDeniedException;
import com.campusone.infrastructure.dto.request.TestEmailRequest;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.user.entity.AccountStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminDiagnosticsControllerTest {

    private static final UUID ADMIN_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000010");

    @Mock
    private NoteAdminAuthorizationService authorizationService;

    @Mock
    private PasswordResetMailer mailer;

    private AdminDiagnosticsController controller;
    private CampusOneUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        controller = new AdminDiagnosticsController(
                authorizationService,
                mailer);
        adminPrincipal = new CampusOneUserPrincipal(
                ADMIN_ID,
                "admin@example.com",
                "$2a$12$password",
                AccountStatus.ACTIVE,
                Set.of("ADMIN"));
    }

    @Test
    void sendTestEmailDefaultsToCurrentAdminEmail() {
        var response = controller.sendTestEmail(adminPrincipal, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Test email sent successfully.");
        verify(authorizationService).requireAdmin(
                ADMIN_ID,
                "admin@example.com");
        verify(mailer).sendTestEmail("admin@example.com");
    }

    @Test
    void sendTestEmailCanUseValidatedRequestEmail() {
        var response = controller.sendTestEmail(
                adminPrincipal,
                new TestEmailRequest("ops@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(mailer).sendTestEmail("ops@example.com");
    }

    @Test
    void sendTestEmailNormalUserIsRejectedByAdminAuthorization() {
        doThrow(new NoteManagementAccessDeniedException())
                .when(authorizationService)
                .requireAdmin(ADMIN_ID, "admin@example.com");

        assertThatThrownBy(() -> controller.sendTestEmail(
                adminPrincipal,
                null))
                .isInstanceOf(NoteManagementAccessDeniedException.class);
    }

    @Test
    void sendTestEmailMailFailureReturnsSafeMessage() {
        doThrow(new IllegalStateException("Resend request failed"))
                .when(mailer)
                .sendTestEmail("admin@example.com");

        var response = controller.sendTestEmail(adminPrincipal, null);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Test email failed. Check Render email provider environment variables.");
    }
}
