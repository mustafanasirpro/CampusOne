package com.campusone.ai.repository;

import com.campusone.ai.entity.AiGeneratedItem;
import com.campusone.ai.entity.AiGeneratedItemType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiGeneratedItemRepository
        extends JpaRepository<AiGeneratedItem, UUID> {

    @Query("""
            select item
            from AiGeneratedItem item
            where item.user.id = :userId
              and item.deleted = false
              and (:itemType is null or item.itemType = :itemType)
            """)
    Page<AiGeneratedItem> findOwnedItems(
            @Param("userId") UUID userId,
            @Param("itemType") AiGeneratedItemType itemType,
            Pageable pageable);

    @Query("""
            select item
            from AiGeneratedItem item
            where item.id = :itemId
              and item.deleted = false
            """)
    Optional<AiGeneratedItem> findActiveById(
            @Param("itemId") UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from AiGeneratedItem item
            where item.id = :itemId
              and item.deleted = false
            """)
    Optional<AiGeneratedItem> findActiveByIdForUpdate(
            @Param("itemId") UUID itemId);
}
