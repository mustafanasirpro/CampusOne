package com.campusone.note.service;

import com.campusone.common.exception.UploadLimitExceededException;
import com.campusone.note.repository.UploadQuotaRepository;
import com.campusone.note.storage.StorageProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadQuotaService {

    private static final long GLOBAL_QUOTA_LOCK = 4_845_673_274_128_474_449L;
    private static final long USER_LOCK_NAMESPACE = 2_823_361_591_588_939_093L;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final UploadQuotaRepository repository;
    private final StorageProperties properties;
    private final Clock clock;

    public UploadQuotaService(
            UploadQuotaRepository repository,
            StorageProperties properties,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enforce(UUID userId, long pendingUploadBytes) {
        repository.acquireTransactionLock(GLOBAL_QUOTA_LOCK);
        repository.acquireTransactionLock(userLockKey(userId));

        ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
        Instant dayStart = now.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant nextDayStart = dayStart.plusSeconds(24L * 60L * 60L);
        YearMonth month = YearMonth.from(now);
        Instant monthStart = month.atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant nextMonthStart = month.plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        long uploadsToday = repository.countUserUploads(
                userId,
                dayStart,
                nextDayStart);
        if (uploadsToday >= properties.getAdminMaxUploadsPerDay()) {
            throw new UploadLimitExceededException(
                    "Daily upload limit reached.");
        }

        long userMonthlyBytes = repository.sumUserUploadBytes(
                userId,
                monthStart,
                nextMonthStart);
        if (exceeds(
                userMonthlyBytes,
                pendingUploadBytes,
                properties.getAdminMaxStorageMbPerMonth())) {
            throw new UploadLimitExceededException(
                    "Monthly upload limit reached.");
        }

        long globalMonthlyBytes = repository.sumGlobalUploadBytes(
                monthStart,
                nextMonthStart);
        if (exceeds(
                globalMonthlyBytes,
                pendingUploadBytes,
                properties.getGlobalUploadStorageCapMb())) {
            throw new UploadLimitExceededException(
                    "Upload limit reached for this month.");
        }
    }

    private boolean exceeds(long currentBytes, long pendingBytes, int limitMb) {
        long limitBytes = limitMb * BYTES_PER_MEGABYTE;
        return pendingBytes > limitBytes
                || currentBytes > limitBytes - pendingBytes;
    }

    private long userLockKey(UUID userId) {
        return userId.getMostSignificantBits()
                ^ userId.getLeastSignificantBits()
                ^ USER_LOCK_NAMESPACE;
    }
}
