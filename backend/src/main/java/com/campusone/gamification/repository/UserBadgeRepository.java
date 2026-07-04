package com.campusone.gamification.repository;

import com.campusone.gamification.entity.UserBadge;
import com.campusone.gamification.entity.UserBadgeId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeRepository
        extends JpaRepository<UserBadge, UserBadgeId> {

    @EntityGraph(attributePaths = {"badge"})
    @Query("""
            select userBadge
            from UserBadge userBadge
            where userBadge.user.id = :userId
            order by
                userBadge.badge.sortOrder asc,
                userBadge.badge.xpRequired asc,
                userBadge.awardedAt asc
            """)
    List<UserBadge> findDetailedByUserId(
            @Param("userId") UUID userId);
}
