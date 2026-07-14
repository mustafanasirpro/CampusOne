package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.lostfound.dto.request.CompleteLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.CreateLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.CreateLostFoundItemRequest;
import com.campusone.lostfound.dto.request.ReviewLostFoundClaimRequest;
import com.campusone.lostfound.dto.request.UpdateLostFoundClaimContactPhoneRequest;
import com.campusone.lostfound.dto.request.UpdateLostFoundItemRequest;
import com.campusone.lostfound.dto.response.LostFoundClaimResponse;
import com.campusone.lostfound.dto.response.LostFoundItemDetailResponse;
import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundClaim;
import com.campusone.lostfound.entity.LostFoundClaimStatus;
import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemImage;
import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.exception.LostFoundConflictException;
import com.campusone.lostfound.mapper.LostFoundMapper;
import com.campusone.lostfound.repository.LostFoundClaimRepository;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.repository.LostFoundMatchRepository;
import com.campusone.moderation.service.ModeratorAuthorizationService;
import com.campusone.note.entity.StorageProvider;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
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

    @Mock
    private ModeratorAuthorizationService moderatorAuthorizationService;

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
                moderatorAuthorizationService,
                new LostFoundContactPhoneNormalizer(),
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
    void createItem_deletesAlreadyUploadedImagesWhenLaterUploadFails() {
        StoredObject firstObject = storedObject("first.jpg");
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(storageService.uploadLostFoundImage(any(), any()))
                .thenReturn(firstObject)
                .thenThrow(new RuntimeException("R2 unavailable"));

        assertThatThrownBy(() -> service.createItem(
                REPORTER_ID,
                createRequest(LostFoundItemType.LOST),
                List.of(jpeg("first.jpg"), jpeg("second.jpg"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("R2 unavailable");

        verify(storageService).delete(firstObject);
    }

    @Test
    void updateItem_deletesReplacedImageObjectsAfterSuccessfulReplacement() {
        LostFoundItem item = publishedFoundItem();
        StoredObject oldObject = storedObject("old.jpg");
        item.replaceImages(List.of(new LostFoundItemImage(oldObject, 0)));
        StoredObject newObject = storedObject("new.jpg");
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(itemRepository.findDetailedById(ITEM_ID)).thenReturn(Optional.of(item));
        when(storageService.uploadLostFoundImage(any(), any()))
                .thenReturn(newObject);

        LostFoundItemDetailResponse response = service.updateItem(
                REPORTER_ID,
                ITEM_ID,
                new UpdateLostFoundItemRequest(
                        null,
                        null,
                        "Updated laptop bag",
                        "Updated description with enough detail.",
                        null,
                        null,
                        null,
                        null),
                List.of(jpeg("new.jpg")));

        assertThat(response.status()).isEqualTo(LostFoundItemStatus.PENDING_REVIEW);
        verify(storageService).delete(oldObject);
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
                claimRequest("+92 300 1234567", true));

        assertThat(response.status()).isEqualTo(LostFoundClaimStatus.PENDING);
        assertThat(response.contactPhone()).isEqualTo("+923001234567");
        assertThat(response.contactPhoneVisible()).isTrue();
        assertThat(item.getStatus()).isEqualTo(LostFoundItemStatus.PUBLISHED);
        verify(integrationService).lostFoundClaimCreated(
                REPORTER_ID,
                CLAIMANT_ID,
                ITEM_ID,
                CLAIM_ID,
                "Black laptop bag");
    }

    @Test
    void createClaim_rejectsMissingContactSharingConsent() {
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

        assertThatThrownBy(() -> service.createClaim(
                CLAIMANT_ID,
                ITEM_ID,
                claimRequest("+923001234567", false)))
                .isInstanceOf(LostFoundConflictException.class)
                .hasMessage(
                        "Agree to share your handover contact number after approval.");
    }

    @Test
    void listItemClaims_masksPendingContactPhoneForReporter() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW));
        when(itemRepository.findDetailedById(ITEM_ID)).thenReturn(Optional.of(item));
        when(claimRepository.findForItem(ITEM_ID, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(
                        List.of(claim),
                        PageRequest.of(0, 20),
                        1));

        LostFoundClaimResponse response = service.listItemClaims(
                REPORTER_ID,
                ITEM_ID,
                0,
                20).content().getFirst();

        assertThat(response.contactPhone()).isNull();
        assertThat(response.maskedContactPhone()).isEqualTo("+92••••••4567");
        assertThat(response.contactPhoneVisible()).isFalse();
    }

    @Test
    void approveClaim_returnsFullContactPhoneToReporter() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW));
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

        assertThat(response.contactPhone()).isEqualTo("+923001234567");
        assertThat(response.contactPhoneVisible()).isTrue();
    }

    @Test
    void rejectClaim_doesNotReturnFullContactPhoneToReporter() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW));
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(claim));
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(claimRepository.existsByItemIdAndStatusIn(
                ITEM_ID,
                EnumSet.of(
                        LostFoundClaimStatus.PENDING,
                        LostFoundClaimStatus.APPROVED)))
                .thenReturn(false);

        LostFoundClaimResponse response = service.rejectClaim(
                REPORTER_ID,
                CLAIM_ID,
                new ReviewLostFoundClaimRequest("Not enough proof."));

        assertThat(response.contactPhone()).isNull();
        assertThat(response.maskedContactPhone()).isEqualTo("+92••••••4567");
        assertThat(response.contactPhoneVisible()).isFalse();
    }

    @Test
    void updateClaimContactPhone_allowsPendingClaimantCorrection() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW.minusSeconds(60)));
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(claim));

        LostFoundClaimResponse response = service.updateClaimContactPhone(
                CLAIMANT_ID,
                CLAIM_ID,
                new UpdateLostFoundClaimContactPhoneRequest(
                        "03 111 222 333",
                        true));

        assertThat(response.contactPhone()).isEqualTo("+923111222333");
        assertThat(claim.getClaimantContactPhone()).isEqualTo("+923111222333");
        assertThat(claim.getContactShareConsentAt()).isEqualTo(NOW);
    }

    @Test
    void updateClaimContactPhone_rejectsNonClaimant() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW));
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.updateClaimContactPhone(
                REPORTER_ID,
                CLAIM_ID,
                new UpdateLostFoundClaimContactPhoneRequest(
                        "+923111222333",
                        true)))
                .isInstanceOf(org.springframework.security.access
                        .AccessDeniedException.class);
    }

    @Test
    void updateClaimContactPhone_rejectsApprovedClaim() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached.",
                "+923001234567",
                NOW));
        claim.approve(reporter, "Verified.", NOW);
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> service.updateClaimContactPhone(
                CLAIMANT_ID,
                CLAIM_ID,
                new UpdateLostFoundClaimContactPhoneRequest(
                        "+923111222333",
                        true)))
                .isInstanceOf(LostFoundConflictException.class)
                .hasMessage("Only a pending claim contact number can be updated.");
    }

    @Test
    void legacyClaimWithoutContactPhoneCanStillBeMapped() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim claim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached."));

        LostFoundClaimResponse response =
                new LostFoundMapper(storageService).toClaim(
                        claim,
                        true,
                        CLAIMANT_ID);

        assertThat(response.contactPhone()).isNull();
        assertThat(response.maskedContactPhone()).isNull();
        assertThat(response.contactPhoneVisible()).isFalse();
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

    @Test
    void completeClaim_rejectsRemainingPendingClaims() {
        LostFoundItem item = publishedFoundItem();
        LostFoundClaim approvedClaim = persistClaim(new LostFoundClaim(
                item,
                claimant,
                "The zipper has a blue keychain attached."));
        approvedClaim.approve(reporter, "Verified privately.", NOW);
        LostFoundClaim pendingClaim = persistClaimWithId(
                new LostFoundClaim(
                        item,
                        user(
                                UUID.fromString("10000000-0000-4000-8000-000000000003"),
                                "other@example.com",
                                university,
                                claimant.getStudentProfile().getDepartment()),
                        "The front pocket contains my student card."),
                UUID.fromString("30000000-0000-4000-8000-000000000002"));
        when(claimRepository.findDetailedById(CLAIM_ID))
                .thenReturn(Optional.of(approvedClaim));
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));
        when(claimRepository.findByItemIdAndStatus(
                ITEM_ID,
                LostFoundClaimStatus.PENDING))
                .thenReturn(List.of(pendingClaim));

        service.completeClaim(
                REPORTER_ID,
                CLAIM_ID,
                new CompleteLostFoundClaimRequest("Returned safely."));

        assertThat(approvedClaim.getStatus()).isEqualTo(LostFoundClaimStatus.COMPLETED);
        assertThat(item.getStatus()).isEqualTo(LostFoundItemStatus.RESOLVED);
        assertThat(pendingClaim.getStatus()).isEqualTo(LostFoundClaimStatus.REJECTED);
        assertThat(pendingClaim.getReviewerNote()).isEqualTo("Item was resolved.");
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

    private CreateLostFoundClaimRequest claimRequest(
            String contactPhone,
            boolean consent) {
        return new CreateLostFoundClaimRequest(
                "The zipper has a blue keychain attached.",
                contactPhone,
                consent);
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
        return persistClaimWithId(claim, CLAIM_ID);
    }

    private LostFoundClaim persistClaimWithId(
            LostFoundClaim claim,
            UUID claimId) {
        ReflectionTestUtils.setField(claim, "id", claimId);
        ReflectionTestUtils.setField(claim, "createdAt", NOW);
        ReflectionTestUtils.setField(claim, "updatedAt", NOW);
        return claim;
    }

    private MockMultipartFile jpeg(String filename) {
        return new MockMultipartFile(
                "images",
                filename,
                "image/jpeg",
                new byte[] {
                    (byte) 0xFF,
                    (byte) 0xD8,
                    (byte) 0xFF,
                    0x00
                });
    }

    private StoredObject storedObject(String filename) {
        return new StoredObject(
                StorageProvider.S3_COMPATIBLE,
                "campusone",
                "lost-found/" + filename,
                filename,
                "image/jpeg",
                4L,
                "a".repeat(64));
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
