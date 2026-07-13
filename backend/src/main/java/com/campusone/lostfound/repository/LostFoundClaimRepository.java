package com.campusone.lostfound.repository;

import com.campusone.lostfound.entity.LostFoundClaim;
import com.campusone.lostfound.entity.LostFoundClaimStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostFoundClaimRepository
        extends JpaRepository<LostFoundClaim, UUID> {

    boolean existsByItemIdAndClaimantIdAndStatusIn(
            UUID itemId,
            UUID claimantUserId,
            Collection<LostFoundClaimStatus> statuses);

    @EntityGraph(attributePaths = {
        "item",
        "item.reporter",
        "item.reporter.studentProfile",
        "item.university",
        "claimant",
        "claimant.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select claim
            from LostFoundClaim claim
            where claim.claimant.id = :userId
               or claim.item.reporter.id = :userId
            """)
    Page<LostFoundClaim> findRelatedToUser(
            @Param("userId") UUID userId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "item",
        "item.reporter",
        "item.reporter.studentProfile",
        "item.university",
        "claimant",
        "claimant.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    @Query("""
            select claim
            from LostFoundClaim claim
            where claim.item.id = :itemId
            order by claim.createdAt desc
            """)
    Page<LostFoundClaim> findForItem(
            @Param("itemId") UUID itemId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "item",
        "item.reporter",
        "item.reporter.studentProfile",
        "item.university",
        "claimant",
        "claimant.studentProfile",
        "reviewedBy",
        "reviewedBy.studentProfile"
    })
    Optional<LostFoundClaim> findDetailedById(UUID id);

    boolean existsByItemIdAndStatusIn(
            UUID itemId,
            Collection<LostFoundClaimStatus> statuses);
}
