package com.campusone.note.repository;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

@Repository
public class UploadQuotaRepository {

    private final EntityManager entityManager;

    public UploadQuotaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void acquireTransactionLock(long lockKey) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, lockKey);
                statement.execute();
            }
        });
    }

    public long countUserUploads(
            UUID userId,
            Instant startInclusive,
            Instant endExclusive) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM file_assets
                        WHERE owner_id = :userId
                          AND storage_provider = 'S3_COMPATIBLE'
                          AND status = 'READY'
                          AND created_at >= :startInclusive
                          AND created_at < :endExclusive
                        """)
                .setParameter("userId", userId)
                .setParameter("startInclusive", startInclusive)
                .setParameter("endExclusive", endExclusive)
                .getSingleResult();
        return count.longValue();
    }

    public long sumUserUploadBytes(
            UUID userId,
            Instant startInclusive,
            Instant endExclusive) {
        Number total = (Number) entityManager.createNativeQuery("""
                        SELECT COALESCE(SUM(size_bytes), 0)
                        FROM file_assets
                        WHERE owner_id = :userId
                          AND storage_provider = 'S3_COMPATIBLE'
                          AND status = 'READY'
                          AND created_at >= :startInclusive
                          AND created_at < :endExclusive
                        """)
                .setParameter("userId", userId)
                .setParameter("startInclusive", startInclusive)
                .setParameter("endExclusive", endExclusive)
                .getSingleResult();
        return total.longValue();
    }

    public long sumGlobalUploadBytes(
            Instant startInclusive,
            Instant endExclusive) {
        Number total = (Number) entityManager.createNativeQuery("""
                        SELECT COALESCE(SUM(size_bytes), 0)
                        FROM file_assets
                        WHERE storage_provider = 'S3_COMPATIBLE'
                          AND status = 'READY'
                          AND created_at >= :startInclusive
                          AND created_at < :endExclusive
                        """)
                .setParameter("startInclusive", startInclusive)
                .setParameter("endExclusive", endExclusive)
                .getSingleResult();
        return total.longValue();
    }
}
