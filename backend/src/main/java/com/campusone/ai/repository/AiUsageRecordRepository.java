package com.campusone.ai.repository;

import com.campusone.ai.entity.AiUsageFeature;
import com.campusone.ai.entity.AiUsageRecord;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageRecordRepository
        extends JpaRepository<AiUsageRecord, UUID> {

    @Query("""
            select usage
            from AiUsageRecord usage
            where usage.user.id = :userId
              and (:feature is null or usage.feature = :feature)
            """)
    Page<AiUsageRecord> findOwnedUsage(
            @Param("userId") UUID userId,
            @Param("feature") AiUsageFeature feature,
            Pageable pageable);
}
