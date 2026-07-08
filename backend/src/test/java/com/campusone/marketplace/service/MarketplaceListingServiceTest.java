package com.campusone.marketplace.service;

import com.campusone.common.service.CommunityIntegrationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.marketplace.dto.request.CreateMarketplaceListingRequest;
import com.campusone.marketplace.dto.request.MarketplaceImageRequest;
import com.campusone.marketplace.dto.request.MarketplaceListingUpdateStatus;
import com.campusone.marketplace.dto.request.UpdateMarketplaceListingRequest;
import com.campusone.marketplace.dto.response.MarketplaceListingPageResponse;
import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import com.campusone.marketplace.entity.MarketplaceListing;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import com.campusone.marketplace.mapper.MarketplaceListingMapper;
import com.campusone.marketplace.repository.MarketplaceListingRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.storage.StorageService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarketplaceListingServiceTest {

    private static final UUID OWNER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID LISTING_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Mock
    private MarketplaceListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    @Mock
    private MarketplaceImageValidator imageValidator;

    @Mock
    private StorageService storageService;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    private MarketplaceListingService listingService;
    private User owner;
    private MarketplaceListing listing;

    @BeforeEach
    void setUp() {
        owner = user(OWNER_ID, "owner@example.com");
        listing = listing(owner);
        listingService = new MarketplaceListingService(
                listingRepository,
                userRepository,
                new MarketplaceListingMapper(storageService),
                integrationService,
                imageValidator,
                storageService,
                adminAuthorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(adminAuthorizationService.canManage(
                        any(UUID.class),
                        any()))
                .thenReturn(true);
    }

    @Test
    void createListing_validRequest_createsActiveListingWithImages() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(listingRepository.save(any(MarketplaceListing.class)))
                .thenAnswer(invocation -> {
                    MarketplaceListing saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", LISTING_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });

        var response = listingService.createListing(
                OWNER_ID,
                createRequest());

        assertThat(response.id()).isEqualTo(LISTING_ID);
        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.ACTIVE);
        assertThat(response.currency()).isEqualTo("PKR");
        assertThat(response.images()).hasSize(1);
    }

    @Test
    void createListing_normalUserCreatesPendingListingAndNotifiesAdmins() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(adminAuthorizationService.canManage(OWNER_ID, "owner@example.com"))
                .thenReturn(false);
        when(listingRepository.save(any(MarketplaceListing.class)))
                .thenAnswer(invocation -> {
                    MarketplaceListing saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", LISTING_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });

        var response = listingService.createListing(
                OWNER_ID,
                createRequest());

        assertThat(response.status())
                .isEqualTo(MarketplaceListingStatus.PENDING_REVIEW);
        verify(integrationService).marketplaceListingSubmittedForApproval(
                OWNER_ID,
                LISTING_ID,
                "Java Programming Textbook");
    }

    @Test
    void listActiveListings_filtersCategoryAndNormalizedTitle() {
        PageImpl<MarketplaceListing> result = new PageImpl<>(List.of(listing));
        when(listingRepository.findActiveListings(
                eq(MarketplaceListingStatus.ACTIVE),
                eq(MarketplaceCategory.BOOKS),
                eq("%java%"),
                any(Pageable.class)))
                .thenReturn(result);

        MarketplaceListingPageResponse response =
                listingService.listActiveListings(
                        MarketplaceCategory.BOOKS,
                        "  JAVA  ",
                        0,
                        20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(LISTING_ID);
    }

    @Test
    void updateListing_ownerCanUpdateAndMarkSold() {
        when(listingRepository.findDetailedById(LISTING_ID))
                .thenReturn(Optional.of(listing));
        UpdateMarketplaceListingRequest request =
                new UpdateMarketplaceListingRequest(
                        "Java Programming Bundle",
                        null,
                        null,
                        new BigDecimal("2200.00"),
                        null,
                        null,
                        MarketplaceListingUpdateStatus.SOLD,
                        List.of());

        var response = listingService.updateListing(
                OWNER_ID,
                LISTING_ID,
                request);

        assertThat(response.title()).isEqualTo("Java Programming Bundle");
        assertThat(response.price()).isEqualByComparingTo("2200.00");
        assertThat(response.status()).isEqualTo(MarketplaceListingStatus.SOLD);
        assertThat(response.images()).isEmpty();
    }

    @Test
    void updateListing_nonOwnerIsRejected() {
        when(listingRepository.findDetailedById(LISTING_ID))
                .thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.updateListing(
                OTHER_USER_ID,
                LISTING_ID,
                new UpdateMarketplaceListingRequest(
                        "Unauthorized title",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteListing_ownerSoftDeletesListing() {
        when(listingRepository.findDetailedById(LISTING_ID))
                .thenReturn(Optional.of(listing));

        listingService.deleteListing(OWNER_ID, LISTING_ID);

        assertThat(listing.getStatus())
                .isEqualTo(MarketplaceListingStatus.DELETED);
        assertThat(listing.getDeletedAt()).isEqualTo(NOW);
        verify(listingRepository).findDetailedById(LISTING_ID);
    }

    private CreateMarketplaceListingRequest createRequest() {
        return new CreateMarketplaceListingRequest(
                "Java Programming Textbook",
                "A clean textbook suitable for first-year programming courses.",
                MarketplaceCategory.BOOKS,
                new BigDecimal("1800.00"),
                "pkr",
                MarketplaceItemCondition.USED,
                List.of(new MarketplaceImageRequest(
                        "https://example.com/java-book.jpg",
                        "Java textbook cover")));
    }

    private MarketplaceListing listing(User seller) {
        MarketplaceListing result = new MarketplaceListing(
                seller,
                "Java Programming Textbook",
                "A clean textbook suitable for first-year programming courses.",
                MarketplaceCategory.BOOKS,
                new BigDecimal("1800.00"),
                "PKR",
                MarketplaceItemCondition.USED);
        ReflectionTestUtils.setField(result, "id", LISTING_ID);
        ReflectionTestUtils.setField(result, "createdAt", NOW);
        ReflectionTestUtils.setField(result, "updatedAt", NOW);
        return result;
    }

    private User user(UUID id, String email) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
