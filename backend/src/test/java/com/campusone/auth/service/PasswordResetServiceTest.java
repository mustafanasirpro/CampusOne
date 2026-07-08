package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.auth.dto.request.ForgotPasswordRequest;
import com.campusone.auth.dto.request.ResetPasswordRequest;
import com.campusone.auth.entity.PasswordResetToken;
import com.campusone.auth.repository.PasswordResetTokenRepository;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.common.exception.InvalidPasswordResetTokenException;
import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-08T12:00:00Z");
    private static final String RAW_TOKEN =
            "reset-token-with-enough-entropy-for-tests";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetMailer mailer;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTokenTtl(Duration.ofMinutes(30));
        service = new PasswordResetService(
                userRepository,
                tokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                mailer,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SecureRandom(new byte[] {1, 2, 3, 4}));
        user = new User("student@example.com", "$2a$12$old-password");
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void requestReset_existingEmailStoresHashedTokenAndReturnsGenericMessage() {
        when(userRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.requestReset(
                new ForgotPasswordRequest("student@example.com"));

        assertThat(response.message()).isEqualTo(
                PasswordResetService.GENERIC_FORGOT_PASSWORD_MESSAGE);
        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
        assertThat(tokenCaptor.getValue().getExpiresAt())
                .isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        verify(mailer).sendResetLink(eq(user), any(String.class));
    }

    @Test
    void requestReset_unknownEmailStillReturnsGenericMessage() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        var response = service.requestReset(
                new ForgotPasswordRequest("missing@example.com"));

        assertThat(response.message()).isEqualTo(
                PasswordResetService.GENERIC_FORGOT_PASSWORD_MESSAGE);
        verify(tokenRepository, never()).save(any());
        verify(mailer, never()).sendResetLink(any(), any());
    }

    @Test
    void resetPassword_validTokenUpdatesPasswordMarksUsedAndRevokesSessions() {
        PasswordResetToken token = usableToken(NOW.plus(Duration.ofMinutes(30)));
        when(tokenRepository.findByTokenHashForUpdate(
                PasswordResetService.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("SecurePass2"))
                .thenReturn("$2a$12$new-password");

        var response = service.resetPassword(
                new ResetPasswordRequest(RAW_TOKEN, "SecurePass2"));

        assertThat(response.message()).isEqualTo(
                PasswordResetService.RESET_SUCCESS_MESSAGE);
        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$new-password");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(refreshTokenRepository).revokeActiveTokensForUser(USER_ID, NOW);
    }

    @Test
    void resetPassword_missingTokenIsRejected() {
        when(tokenRepository.findByTokenHashForUpdate(
                PasswordResetService.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest(RAW_TOKEN, "SecurePass2")))
                .isInstanceOf(InvalidPasswordResetTokenException.class)
                .hasMessage("This reset link is invalid or expired.");
    }

    @Test
    void resetPassword_expiredTokenIsRejected() {
        PasswordResetToken token = usableToken(NOW.minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(
                PasswordResetService.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest(RAW_TOKEN, "SecurePass2")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void resetPassword_usedTokenIsRejected() {
        PasswordResetToken token = usableToken(NOW.plus(Duration.ofMinutes(30)));
        token.markUsed(NOW.minusSeconds(60));
        when(tokenRepository.findByTokenHashForUpdate(
                PasswordResetService.hashToken(RAW_TOKEN)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest(RAW_TOKEN, "SecurePass2")))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    private PasswordResetToken usableToken(Instant expiresAt) {
        return new PasswordResetToken(
                user,
                PasswordResetService.hashToken(RAW_TOKEN),
                expiresAt,
                NOW);
    }
}
