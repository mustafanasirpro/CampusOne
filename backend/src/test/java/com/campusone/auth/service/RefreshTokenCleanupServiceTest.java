package com.campusone.auth.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void deleteExpiredTokens_scheduledCleanup_usesCurrentUtcInstant() {
        Instant now = Instant.parse("2026-07-01T12:00:00Z");
        when(refreshTokenRepository.deleteExpiredTokens(now)).thenReturn(2);
        RefreshTokenCleanupService cleanupService = new RefreshTokenCleanupService(
                refreshTokenRepository,
                Clock.fixed(now, ZoneOffset.UTC));

        cleanupService.deleteExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(now);
    }
}
