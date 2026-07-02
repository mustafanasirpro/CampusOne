package com.campusone.marketplace.repository;

import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceListing;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceListingRepository
        extends JpaRepository<MarketplaceListing, UUID> {

    @EntityGraph(attributePaths = {
        "seller",
        "seller.studentProfile",
        "seller.studentProfile.university"
    })
    @Query("""
            select listing
            from MarketplaceListing listing
            where listing.deletedAt is null
              and listing.status = :status
              and (:category is null or listing.category = :category)
              and (
                    :searchPattern is null
                    or lower(listing.title) like :searchPattern escape '\\'
              )
            """)
    Page<MarketplaceListing> findActiveListings(
            @Param("status") MarketplaceListingStatus status,
            @Param("category") MarketplaceCategory category,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "seller",
        "seller.studentProfile",
        "seller.studentProfile.university"
    })
    @Query("""
            select listing
            from MarketplaceListing listing
            where listing.deletedAt is null
              and listing.seller.id = :sellerId
            """)
    Page<MarketplaceListing> findMyListings(
            @Param("sellerId") UUID sellerId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "seller",
        "seller.studentProfile",
        "seller.studentProfile.university",
        "images"
    })
    @Query("""
            select listing
            from MarketplaceListing listing
            where listing.id = :listingId
              and listing.deletedAt is null
            """)
    Optional<MarketplaceListing> findDetailedById(
            @Param("listingId") UUID listingId);
}
