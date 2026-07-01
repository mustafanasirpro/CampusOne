package com.campusone.auth.service;

import com.campusone.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenCleanupService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public RefreshTokenCleanupService(
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${app.auth.refresh-token-cleanup-interval:24h}",
            fixedDelayString = "${app.auth.refresh-token-cleanup-interval:24h}")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(clock.instant());
        if (deleted > 0) {
            LOGGER.info("Deleted {} expired refresh-token records", deleted);
        }
    }
}
