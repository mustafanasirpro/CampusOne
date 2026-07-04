package com.campusone.gamification.repository;

import com.campusone.gamification.entity.GamificationProfile;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GamificationProfileRepository
        extends JpaRepository<GamificationProfile, UUID> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO gamification_profiles (user_id)
                    VALUES (:userId)
                    ON CONFLICT (user_id) DO NOTHING
                    """,
            nativeQuery = true)
    int insertIfMissing(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {
        "user",
        "user.studentProfile"
    })
    @Query("""
            select profile
            from GamificationProfile profile
            where profile.userId = :userId
            """)
    Optional<GamificationProfile> findDetailedByUserId(
            @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "user",
        "user.studentProfile"
    })
    @Query("""
            select profile
            from GamificationProfile profile
            where profile.userId = :userId
            """)
    Optional<GamificationProfile> findByUserIdForUpdate(
            @Param("userId") UUID userId);

    @Query(
            value = """
                    SELECT
                        ROW_NUMBER() OVER (
                            ORDER BY
                                profile.total_xp DESC,
                                profile.created_at ASC,
                                profile.user_id ASC
                        ) AS "rank",
                        profile.user_id AS "userId",
                        CASE
                            WHEN student.visibility = 'PUBLIC'
                                THEN student.full_name
                            ELSE NULL
                        END AS "fullName",
                        profile.total_xp::BIGINT AS "totalXpForPeriod",
                        profile.total_xp AS "allTimeXp",
                        profile.level AS "level"
                    FROM gamification_profiles profile
                    LEFT JOIN student_profiles student
                        ON student.user_id = profile.user_id
                    ORDER BY
                        profile.total_xp DESC,
                        profile.created_at ASC,
                        profile.user_id ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM gamification_profiles
                    """,
            nativeQuery = true)
    Page<LeaderboardEntryProjection> findAllTimeLeaderboard(
            Pageable pageable);

    @Query(
            value = """
                    WITH period_totals AS (
                        SELECT
                            xp_entry.user_id,
                            SUM(xp_entry.points)::BIGINT AS period_xp
                        FROM xp_transactions xp_entry
                        WHERE xp_entry.created_at >= :periodStart
                          AND xp_entry.created_at < :periodEnd
                        GROUP BY xp_entry.user_id
                    )
                    SELECT
                        ROW_NUMBER() OVER (
                            ORDER BY
                                period.period_xp DESC,
                                profile.created_at ASC,
                                profile.user_id ASC
                        ) AS "rank",
                        profile.user_id AS "userId",
                        CASE
                            WHEN student.visibility = 'PUBLIC'
                                THEN student.full_name
                            ELSE NULL
                        END AS "fullName",
                        period.period_xp AS "totalXpForPeriod",
                        profile.total_xp AS "allTimeXp",
                        profile.level AS "level"
                    FROM period_totals period
                    JOIN gamification_profiles profile
                        ON profile.user_id = period.user_id
                    LEFT JOIN student_profiles student
                        ON student.user_id = profile.user_id
                    ORDER BY
                        period.period_xp DESC,
                        profile.created_at ASC,
                        profile.user_id ASC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT xp_entry.user_id)
                    FROM xp_transactions xp_entry
                    WHERE xp_entry.created_at >= :periodStart
                      AND xp_entry.created_at < :periodEnd
                    """,
            nativeQuery = true)
    Page<LeaderboardEntryProjection> findPeriodLeaderboard(
            @Param("periodStart") Instant periodStart,
            @Param("periodEnd") Instant periodEnd,
            Pageable pageable);
}
