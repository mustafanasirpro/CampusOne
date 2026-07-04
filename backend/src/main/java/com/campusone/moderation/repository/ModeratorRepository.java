package com.campusone.moderation.repository;

import com.campusone.moderation.entity.Moderator;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModeratorRepository
        extends JpaRepository<Moderator, UUID> {

    @EntityGraph(attributePaths = {
        "user",
        "user.studentProfile"
    })
    @Query("""
            select moderator
            from Moderator moderator
            where moderator.userId = :userId
            """)
    Optional<Moderator> findDetailedByUserId(
            @Param("userId") UUID userId);

    @Query("""
            select moderator.userId
            from Moderator moderator
            where moderator.active = true
            """)
    List<UUID> findActiveModeratorUserIds();
}
