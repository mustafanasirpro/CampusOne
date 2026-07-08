package com.campusone.infrastructure.controller;

import com.campusone.auth.service.PasswordResetMailer;
import com.campusone.infrastructure.dto.request.TestEmailRequest;
import com.campusone.infrastructure.dto.response.TestEmailResponse;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diagnostics")
@Tag(name = "Admin Diagnostics")
@SecurityRequirement(name = "bearerAuth")
public class AdminDiagnosticsController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminDiagnosticsController.class);

    private static final String SUCCESS_MESSAGE =
            "Test email sent successfully.";
    private static final String FAILURE_MESSAGE =
            "Test email failed. Check Render MAIL_* environment variables and SMTP credentials.";

    private final NoteAdminAuthorizationService authorizationService;
    private final PasswordResetMailer mailer;

    public AdminDiagnosticsController(
            NoteAdminAuthorizationService authorizationService,
            PasswordResetMailer mailer) {
        this.authorizationService = authorizationService;
        this.mailer = mailer;
    }

    @PostMapping(
            value = "/test-email",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send an admin-only SMTP diagnostic email")
    public ResponseEntity<TestEmailResponse> sendTestEmail(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody(required = false) TestEmailRequest request) {
        authorizationService.requireAdmin(
                principal.getUserId(),
                principal.getUsername());

        String recipientEmail = recipientEmail(principal, request);
        try {
            mailer.sendTestEmail(recipientEmail);
            return ResponseEntity.ok(new TestEmailResponse(SUCCESS_MESSAGE));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Test email failed for admin {}: {}: {}",
                    principal.getUserId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TestEmailResponse(FAILURE_MESSAGE));
        }
    }

    private String recipientEmail(
            CampusOneUserPrincipal principal,
            TestEmailRequest request) {
        if (request != null && StringUtils.hasText(request.email())) {
            return request.email().trim();
        }
        return principal.getUsername();
    }
}
