package com.campusone.lostfound.repository;

import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostFoundItemRepository
        extends JpaRepository<LostFoundItem, UUID> {

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    @Query("""
            select item
            from LostFoundItem item
            where item.deletedAt is null
              and item.university.id = :universityId
              and item.status = :status
              and (:now < item.expiresAt or item.expiresAt is null)
              and (:type is null or item.type = :type)
              and (:category is null or item.category = :category)
              and (
                    :searchPattern is null
                    or lower(item.title) like :searchPattern escape '\\'
                    or lower(item.description) like :searchPattern escape '\\'
                    or lower(item.locationText) like :searchPattern escape '\\'
                    or lower(coalesce(item.brand, '')) like :searchPattern escape '\\'
                    or lower(coalesce(item.color, '')) like :searchPattern escape '\\'
                    or lower(item.reporter.studentProfile.fullName) like :searchPattern escape '\\'
              )
            """)
    Page<LostFoundItem> findPublishedForUniversity(
            @Param("universityId") UUID universityId,
            @Param("status") LostFoundItemStatus status,
            @Param("now") Instant now,
            @Param("type") LostFoundItemType type,
            @Param("category") LostFoundCategory category,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    @Query("""
            select item
            from LostFoundItem item
            where item.deletedAt is null
              and item.reporter.id = :reporterUserId
            """)
    Page<LostFoundItem> findOwnedItems(
            @Param("reporterUserId") UUID reporterUserId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    Page<LostFoundItem> findAllByStatusAndDeletedAtIsNull(
            LostFoundItemStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    @Query("""
            select item
            from LostFoundItem item
            where item.id = :itemId
              and item.deletedAt is null
            """)
    Optional<LostFoundItem> findDetailedById(@Param("itemId") UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    @Query("""
            select item
            from LostFoundItem item
            where item.id = :itemId
              and item.deletedAt is null
            """)
    Optional<LostFoundItem> findActiveByIdForUpdate(
            @Param("itemId") UUID itemId);

    @EntityGraph(attributePaths = {
        "reporter",
        "reporter.studentProfile",
        "university",
        "images"
    })
    @Query("""
            select item
            from LostFoundItem item
            where item.deletedAt is null
              and item.university.id = :universityId
              and item.type = :oppositeType
              and item.status = :status
              and (:now < item.expiresAt or item.expiresAt is null)
              and item.itemDate between :startDate and :endDate
              and item.id <> :itemId
            order by item.createdAt desc
            """)
    java.util.List<LostFoundItem> findMatchCandidates(
            @Param("itemId") UUID itemId,
            @Param("universityId") UUID universityId,
            @Param("oppositeType") LostFoundItemType oppositeType,
            @Param("status") LostFoundItemStatus status,
            @Param("now") Instant now,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("""
            select count(item)
            from LostFoundItem item
            where item.university.id = :universityId
              and item.status in :statuses
              and item.deletedAt is null
            """)
    long countByUniversityAndStatusIn(
            @Param("universityId") UUID universityId,
            @Param("statuses") Collection<LostFoundItemStatus> statuses);

    @Query("""
            select item.status, count(item)
            from LostFoundItem item
            where item.deletedAt is null
            group by item.status
            """)
    java.util.List<Object[]> countByStatus();

    boolean existsByIdAndStatusInAndDeletedAtIsNull(
            UUID id,
            Set<LostFoundItemStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from LostFoundItem item
            where item.deletedAt is null
              and item.status = :status
              and item.expiresAt is not null
              and item.expiresAt <= :now
            order by item.expiresAt asc, item.id asc
            """)
    java.util.List<LostFoundItem> findExpiredPublishedForUpdate(
            @Param("status") LostFoundItemStatus status,
            @Param("now") Instant now,
            Pageable pageable);
}
