package com.campusone.gamification.repository;

import com.campusone.gamification.entity.Badge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    List<Badge> findByActiveTrueOrderBySortOrderAscXpRequiredAsc();

    @Query("""
            select badge
            from Badge badge
            where badge.active = true
              and badge.xpRequired <= :totalXp
              and not exists (
                    select userBadge.id
                    from UserBadge userBadge
                    where userBadge.badge = badge
                      and userBadge.user.id = :userId
              )
            order by badge.sortOrder asc, badge.xpRequired asc
            """)
    List<Badge> findEligibleUnawardedBadges(
            @Param("userId") UUID userId,
            @Param("totalXp") int totalXp);
}
