package com.campusone.auth.service;

import com.campusone.auth.dto.request.ForgotPasswordRequest;
import com.campusone.auth.dto.request.ResetPasswordRequest;
import com.campusone.auth.dto.response.PasswordResetResponse;
import com.campusone.auth.entity.PasswordResetToken;
import com.campusone.auth.repository.PasswordResetTokenRepository;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.common.exception.InvalidPasswordResetTokenException;
import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    public static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "If an account exists, password reset instructions have been sent.";
    public static final String RESET_SUCCESS_MESSAGE =
            "Password reset successfully. You can now log in.";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetMailer mailer,
            PasswordResetProperties properties,
            Clock clock) {
        this(
                userRepository,
                tokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                mailer,
                properties,
                clock,
                new SecureRandom());
    }

    PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetMailer mailer,
            PasswordResetProperties properties,
            Clock clock,
            SecureRandom secureRandom) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public PasswordResetResponse requestReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email())
                .ifPresent(this::createAndSendToken);
        return new PasswordResetResponse(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository
                .findByTokenHashForUpdate(hashToken(request.token()))
                .filter(resetToken -> resetToken.isUsableAt(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLoginAttempts();
        token.markUsed(now);
        refreshTokenRepository.revokeActiveTokensForUser(user.getId(), now);
        return new PasswordResetResponse(RESET_SUCCESS_MESSAGE);
    }

    private void createAndSendToken(User user) {
        Instant now = clock.instant();
        String rawToken = randomToken();
        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                hashToken(rawToken),
                now.plus(properties.getTokenTtl()),
                now);
        tokenRepository.save(resetToken);
        try {
            mailer.sendResetLink(user, rawToken);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to send password reset email for user {}: {}: {}",
                    user.getId(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.trim().getBytes(
                            java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
