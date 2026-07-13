package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.lostfound.dto.request.CreateLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.CreateLostFoundItemRequest;
import com.campusone.lostfound.dto.request.ReviewLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.UpdateLostFoundItemRequest;
import com.campusone.lostfound.dto.response.LostFoundClaimResponse;
import com.campusone.lostfound.dto.response.LostFoundItemDetailResponse;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundClaim;
import com.campusone.lostfound.entity.LostFoundClaimStatus;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.exception.LostFoundConflictException;
import com.campusone.lostfound.mapper.LostFoundMapper;
import com.campusone.lostfound.repository.LostFoundClaimRepository;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.repository.LostFoundMatchRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.storage.StorageService;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostFoundServiceTest {

    private static final UUID REPORTER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID CLAIMANT_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID ITEM_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final UUID CLAIM_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-14T08:00:00Z");

    @Mock
    private LostFoundItemRepository itemRepository;

    @Mock
    private LostFoundClaimRepository claimRepository;

    @Mock
    private LostFoundMatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private LostFoundMatchingService matchingService;

    @Mock
    private CommunityIntegrationService integrationService;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    private LostFoundService service;
    private User reporter;
    private User claimant;
    private University university;

    @BeforeEach
    void setUp() {
        university = new University(
                "CampusOne University",
                "COU",
                "Lahore");
        ReflectionTestUtils.setField(
                university,
                "id",
                UUID.fromString("40000000-0000-4000-8000-000000000001"));
        Department department = new Department(
                university,
                "Computer Science",
                "CS");
        ReflectionTestUtils.setField(
                department,
                "id",
                UUID.fromString("50000000-0000-4000-8000-000000000001"));
        reporter = user(REPORTER_ID, "reporter@example.com", university, department);
        claimant = user(CLAIMANT_ID, "claimant@example.com", university, department);
        service = new LostFoundService(
                itemRepository,
                claimRepository,
                matchRepository,
                userRepository,
                new LostFoundMapper(storageService),
                new LostFoundImageValidator(5),
                storageService,
                matchingService,
                integrationService,
                adminAuthorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createItem_derivesReporterAndUniversityAndStartsPendingReview() {
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(itemRepository.save(any(LostFoundItem.class)))
                .thenAnswer(invocation -> persistItem(invocation.getArgument(0)));

        LostFoundItemDetailResponse response = service.createItem(
                REPORTER_ID,
                createRequest(LostFoundItemType.LOST),
                List.of());

        assertThat(response.id()).isEqualTo(ITEM_ID);
        assertThat(response.status()).isEqualTo(LostFoundItemStatus.PENDING_REVIEW);
        assertThat(response.reporter().fullName()).isEqualTo("Reporter");
        assertThat(response.reporter().userId()).isEqualTo(REPORTER_ID);
        verify(integrationService).lostFoundItemSubmittedForApproval(
                REPORTER_ID,
                ITEM_ID,
                "Black laptop bag");
    }

    @Test
    void updateItem_rejectsTypeChangesBecauseTypeIsImmutable() {
        LostFoundItem item = publishedFoundItem();
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(itemRepository.findDetailedById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.updateItem(
                REPORTER_ID,
                ITEM_ID,
                new UpdateLostFoundItemRequest(
                        LostFoundItemType.LOST,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                null))
                .isInstanceOf(LostFoundConflictException.class)
                .hasMessage("Lost and found item type cannot be changed.");
    }

    @Test
    void createClaim_keepsFoundItemPublishedUntilClaimIsApproved() {
        LostFoundItem item = publishedFoundItem();
        when(userRepository.findById(CLAIMANT_ID)).thenReturn(Optional.of(claimant));
        when(itemRepository.findActiveByIdForUpdate(ITEM_ID))
                .thenReturn(Optional.of(item));
        when(claimRepository.existsByItemIdAndClaimantIdAndStatusIn(
                ITEM_ID,
                CLAIMANT_ID,
                EnumSet.of(
                        LostFoundClaimStatus.PENDING,
                        LostFoundClaimStatus.APPROVED)))
                .thenReturn(false);
        when(claimRepository.save(any(LostFoundClaim.class)))
                .thenAnswer(invocation -> persistClaim(invocation.getArgument(0)));

        LostFoundClaimResponse response = service.createClaim(
                CLAIMANT_ID,
                ITEM_ID,
                new CreateLostFoundClaimRequest(
                        "The zipper has a blue keychain attached."));

        assertThat(response.status()).isEqualTo(LostFoundClaimStatus.PENDING);
        assertThat(item.getStatus()).isEqualTo(LostFoundItemStatus.PUBLISHED);
        verify(integrationService).lostFoundClaimCreated(
                REPORTER_ID,
                CLAIMANT_ID,
                ITEM_ID,
                CLAIM_ID,
                "Black laptop bag");
    }

    @Test
    void approveClaim_movesFoundItemIntoClaimInProgress() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached."));
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(claim));
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(claimRepository.existsByItemIdAndStatusIn(
                ITEM_ID,
                EnumSet.of(LostFoundClaimStatus.APPROVED)))
                .thenReturn(false);

        LostFoundClaimResponse response = service.approveClaim(
                REPORTER_ID,
                CLAIM_ID,
                new ReviewLostFoundClaimRequest("Verified privately."));

        assertThat(response.status()).isEqualTo(LostFoundClaimStatus.APPROVED);
        assertThat(item.getStatus())
                .isEqualTo(LostFoundItemStatus.CLAIM_IN_PROGRESS);
        verify(integrationService).lostFoundClaimReviewed(
                CLAIMANT_ID,
                REPORTER_ID,
                ITEM_ID,
                CLAIM_ID,
                "Black laptop bag",
                true);
    }

    private CreateLostFoundItemRequest createRequest(LostFoundItemType type) {
        return new CreateLostFoundItemRequest(
                type,
                LostFoundCategory.ELECTRONICS,
                "Black laptop bag",
                "A black laptop bag with a small front pocket.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
    }

    private LostFoundItem publishedFoundItem() {
        LostFoundItem item = new LostFoundItem(
                reporter,
                university,
                LostFoundItemType.FOUND,
                LostFoundCategory.ELECTRONICS,
                "Black laptop bag",
                "A black laptop bag with a small front pocket.",
                "Main library entrance",
                LocalDate.of(2026, 7, 13),
                "Dell",
                "Black");
        persistItem(item);
        item.approve(reporter, NOW, NOW.plusSeconds(90L * 24 * 60 * 60));
        return item;
    }

    private LostFoundItem persistItem(LostFoundItem item) {
        ReflectionTestUtils.setField(item, "id", ITEM_ID);
        ReflectionTestUtils.setField(item, "createdAt", NOW);
        ReflectionTestUtils.setField(item, "updatedAt", NOW);
        return item;
    }

    private LostFoundClaim persistClaim(LostFoundClaim claim) {
        ReflectionTestUtils.setField(claim, "id", CLAIM_ID);
        ReflectionTestUtils.setField(claim, "createdAt", NOW);
        ReflectionTestUtils.setField(claim, "updatedAt", NOW);
        return claim;
    }

    private User user(
            UUID id,
            String email,
            University university,
            Department department) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        user.setStudentProfile(new StudentProfile(
                user,
                university,
                department,
                email.startsWith("reporter") ? "Reporter" : "Claimant",
                4));
        return user;
    }
}
