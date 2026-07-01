package com.campusone.auth.service;

import com.campusone.auth.entity.RefreshToken;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.common.exception.InvalidRefreshTokenException;
import com.campusone.config.AuthSessionProperties;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int ENCODED_TOKEN_LENGTH = 43;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthSessionProperties properties,
            Clock clock) {
        this(
                refreshTokenRepository,
                properties,
                clock,
                new SecureRandom());
    }

    RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AuthSessionProperties properties,
            Clock clock,
            SecureRandom secureRandom) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return createAndSave(user, UUID.randomUUID(), clock.instant());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken currentToken = findForUpdate(rawToken);
        if (currentToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveTokenFamily(
                    currentToken.getTokenFamily(),
                    now);
            throw new InvalidRefreshTokenException();
        }
        if (!currentToken.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }
        if (currentToken.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            refreshTokenRepository.revokeActiveTokenFamily(
                    currentToken.getTokenFamily(),
                    now);
            throw new InvalidRefreshTokenException();
        }

        IssuedRefreshToken issued = createAndSave(
                currentToken.getUser(),
                currentToken.getTokenFamily(),
                now);
        currentToken.replaceWith(issued.refreshToken(), now);
        return issued;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (!hasValidFormat(rawToken)) {
            return;
        }

        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revokeActiveTokenFamily(
                        token.getTokenFamily(),
                        clock.instant()));
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private IssuedRefreshToken createAndSave(
            User user,
            UUID tokenFamily,
            Instant now) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = now.plus(properties.getRefreshTokenTtl());

        RefreshToken token = new RefreshToken(
                user,
                hash(rawToken),
                tokenFamily,
                expiresAt,
                now);
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(rawToken, expiresAt, user, token);
    }

    private RefreshToken findForUpdate(String rawToken) {
        if (!hasValidFormat(rawToken)) {
            throw new InvalidRefreshTokenException();
        }
        return refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    private boolean hasValidFormat(String rawToken) {
        return rawToken != null
                && rawToken.length() == ENCODED_TOKEN_LENGTH
                && TOKEN_PATTERN.matcher(rawToken).matches();
    }
}
