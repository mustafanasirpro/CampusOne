package com.campusone.lostfound.repository;

import com.campusone.lostfound.entity.LostFoundMatch;
import com.campusone.lostfound.entity.LostFoundMatchStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostFoundMatchRepository
        extends JpaRepository<LostFoundMatch, UUID> {

    @EntityGraph(attributePaths = {
        "lostItem",
        "lostItem.reporter",
        "lostItem.reporter.studentProfile",
        "foundItem",
        "foundItem.reporter",
        "foundItem.reporter.studentProfile"
    })
    Optional<LostFoundMatch> findByLostItemIdAndFoundItemId(
            UUID lostItemId,
            UUID foundItemId);

    @EntityGraph(attributePaths = {
        "lostItem",
        "lostItem.reporter",
        "lostItem.reporter.studentProfile",
        "foundItem",
        "foundItem.reporter",
        "foundItem.reporter.studentProfile"
    })
    Optional<LostFoundMatch> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {
        "lostItem",
        "lostItem.reporter",
        "lostItem.reporter.studentProfile",
        "foundItem",
        "foundItem.reporter",
        "foundItem.reporter.studentProfile"
    })
    @Query("""
            select match
            from LostFoundMatch match
            where (match.lostItem.reporter.id = :userId
                    or match.foundItem.reporter.id = :userId)
              and (:status is null or match.status = :status)
            order by match.score desc, match.createdAt desc
            """)
    Page<LostFoundMatch> findRelatedToUser(
            @Param("userId") UUID userId,
            @Param("status") LostFoundMatchStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "lostItem",
        "lostItem.reporter",
        "lostItem.reporter.studentProfile",
        "foundItem",
        "foundItem.reporter",
        "foundItem.reporter.studentProfile"
    })
    @Query("""
            select match
            from LostFoundMatch match
            where (match.lostItem.id = :itemId
                    or match.foundItem.id = :itemId)
              and (:status is null or match.status = :status)
            order by match.score desc, match.createdAt desc
            """)
    Page<LostFoundMatch> findForItem(
            @Param("itemId") UUID itemId,
            @Param("status") LostFoundMatchStatus status,
            Pageable pageable);
}
