package com.campusone.note.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.UploadLimitExceededException;
import com.campusone.note.repository.UploadQuotaRepository;
import com.campusone.note.storage.StorageProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadQuotaServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final long ONE_MB = 1024L * 1024L;

    @Mock
    private UploadQuotaRepository repository;

    private StorageProperties properties;
    private UploadQuotaService service;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        service = new UploadQuotaService(
                repository,
                properties,
                Clock.fixed(
                        Instant.parse("2026-07-05T12:00:00Z"),
                        ZoneOffset.UTC));
    }

    @Test
    void enforce_atDailyLimit_rejectsUpload() {
        when(repository.countUserUploads(
                USER_ID,
                Instant.parse("2026-07-05T00:00:00Z"),
                Instant.parse("2026-07-06T00:00:00Z")))
                .thenReturn(200L);

        assertThatThrownBy(() -> service.enforce(USER_ID, ONE_MB))
                .isInstanceOf(UploadLimitExceededException.class)
                .hasMessage("Daily upload limit reached.");
    }

    @Test
    void enforce_uploadWouldExceedAdminMonthlyLimit_rejectsUpload() {
        when(repository.sumUserUploadBytes(
                USER_ID,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .thenReturn(5000L * ONE_MB);

        assertThatThrownBy(() -> service.enforce(USER_ID, ONE_MB))
                .isInstanceOf(UploadLimitExceededException.class)
                .hasMessage("Monthly upload limit reached.");
    }

    @Test
    void enforce_uploadWouldExceedGlobalMonthlyCap_rejectsUpload() {
        when(repository.sumGlobalUploadBytes(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .thenReturn(8192L * ONE_MB);

        assertThatThrownBy(() -> service.enforce(USER_ID, ONE_MB))
                .isInstanceOf(UploadLimitExceededException.class)
                .hasMessage("Upload limit reached for this month.");
    }

    @Test
    void enforce_belowAllLimits_allowsUploadAndAcquiresDatabaseLocks() {
        when(repository.countUserUploads(
                USER_ID,
                Instant.parse("2026-07-05T00:00:00Z"),
                Instant.parse("2026-07-06T00:00:00Z")))
                .thenReturn(199L);
        when(repository.sumUserUploadBytes(
                USER_ID,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .thenReturn(4975L * ONE_MB);
        when(repository.sumGlobalUploadBytes(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z")))
                .thenReturn(8000L * ONE_MB);

        assertThatCode(() -> service.enforce(USER_ID, 25L * ONE_MB))
                .doesNotThrowAnyException();

        verify(repository).acquireTransactionLock(
                4_845_673_274_128_474_449L);
    }
}
