package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.auth.entity.RefreshToken;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.common.exception.InvalidRefreshTokenException;
import com.campusone.config.AuthSessionProperties;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.User;
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

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");
    private static final String RAW_TOKEN = "A".repeat(43);

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private AuthSessionProperties properties;
    private User user;

    @BeforeEach
    void setUp() {
        properties = new AuthSessionProperties();
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new SecureRandom());
        user = new User(
                "ali.khan@example.com",
                "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        user.setAccountStatus(AccountStatus.ACTIVE);
    }

    @Test
    void issue_activeUser_storesOnlyHashAndReturnsOpaqueToken() {
        IssuedRefreshToken issued = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken stored = tokenCaptor.getValue();

        assertThat(issued.token()).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(stored.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(issued.token())
                .isEqualTo(refreshTokenService.hash(issued.token()));
        assertThat(stored.getUser()).isSameAs(user);
        assertThat(stored.getTokenFamily()).isNotNull();
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(issued.expiresAt()).isEqualTo(stored.getExpiresAt());
    }

    @Test
    void rotate_validToken_revokesOldTokenAndPersistsReplacement() {
        RefreshToken current = tokenFor(user, NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(current));

        IssuedRefreshToken rotated = refreshTokenService.rotate(RAW_TOKEN);

        assertThat(current.getRevokedAt()).isEqualTo(NOW);
        assertThat(current.getReplacedByToken()).isSameAs(rotated.refreshToken());
        assertThat(rotated.refreshToken().getTokenFamily())
                .isEqualTo(current.getTokenFamily());
        assertThat(rotated.token()).isNotEqualTo(RAW_TOKEN);
        assertThat(rotated.user()).isSameAs(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_expiredToken_rejectsSessionWithoutIssuingReplacement() {
        RefreshToken expired = tokenFor(user, NOW);
        when(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeActiveTokenFamily(any(), any());
    }

    @Test
    void rotate_revokedToken_rejectsSessionWithoutIssuingReplacement() {
        RefreshToken revoked = tokenFor(user, NOW.plus(Duration.ofDays(1)));
        revoked.revoke(NOW.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository).revokeActiveTokenFamily(
                revoked.getTokenFamily(),
                NOW);
    }

    @Test
    void rotate_suspendedUser_rejectsSessionWithoutIssuingReplacement() {
        user.setAccountStatus(AccountStatus.SUSPENDED);
        RefreshToken current = tokenFor(user, NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> refreshTokenService.rotate(RAW_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository).revokeActiveTokenFamily(
                current.getTokenFamily(),
                NOW);
    }

    @Test
    void rotate_malformedToken_rejectsWithoutDatabaseLookup() {
        assertThatThrownBy(() -> refreshTokenService.rotate("not-a-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
    }

    @Test
    void revoke_validToken_marksOnlyMatchingSessionRevoked() {
        RefreshToken current = tokenFor(user, NOW.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(current));

        refreshTokenService.revoke(RAW_TOKEN);

        verify(refreshTokenRepository).revokeActiveTokenFamily(
                current.getTokenFamily(),
                NOW);
    }

    @Test
    void revoke_missingOrMalformedToken_isIdempotent() {
        refreshTokenService.revoke(null);
        refreshTokenService.revoke("invalid");

        verify(refreshTokenRepository, never()).findByTokenHashForUpdate(any());
    }

    private RefreshToken tokenFor(User tokenUser, Instant expiresAt) {
        return new RefreshToken(
                tokenUser,
                refreshTokenService.hash(RAW_TOKEN),
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                expiresAt,
                NOW.minus(Duration.ofDays(1)));
    }
}
