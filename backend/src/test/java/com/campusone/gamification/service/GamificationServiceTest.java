package com.campusone.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.gamification.entity.Badge;
import com.campusone.gamification.entity.GamificationActionType;
import com.campusone.gamification.entity.GamificationProfile;
import com.campusone.gamification.entity.GamificationSourceType;
import com.campusone.gamification.entity.LeaderboardPeriod;
import com.campusone.gamification.entity.UserBadge;
import com.campusone.gamification.entity.XpTransaction;
import com.campusone.gamification.exception.GamificationConflictException;
import com.campusone.gamification.mapper.GamificationMapper;
import com.campusone.gamification.repository.BadgeRepository;
import com.campusone.gamification.repository.GamificationProfileRepository;
import com.campusone.gamification.repository.LeaderboardEntryProjection;
import com.campusone.gamification.repository.UserBadgeRepository;
import com.campusone.gamification.repository.XpTransactionRepository;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID SOURCE_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID TRANSACTION_ID = UUID.fromString(
            "90000000-0000-4000-8000-000000000001");
    private static final UUID BADGE_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000002");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Mock
    private GamificationProfileRepository profileRepository;

    @Mock
    private XpTransactionRepository transactionRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserRepository userRepository;

    private GamificationService gamificationService;
    private User user;
    private GamificationProfile profile;

    @BeforeEach
    void setUp() {
        user = new User(
                "student@example.com",
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        profile = new GamificationProfile(user);
        setProfilePersistenceFields(profile);
        gamificationService = new GamificationService(
                profileRepository,
                transactionRepository,
                badgeRepository,
                userBadgeRepository,
                userRepository,
                new GamificationMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getOrCreateProfile_missingProfile_createsDefaultProfile() {
        stubProfileForRead();
        when(userBadgeRepository.findDetailedByUserId(USER_ID))
                .thenReturn(List.of());

        var response =
                gamificationService.getOrCreateProfile(USER_ID);

        verify(profileRepository).insertIfMissing(USER_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.totalXp()).isZero();
        assertThat(response.level()).isEqualTo(1);
    }

    @Test
    void getPublicProfile_existingUser_returnsSafeProfile() {
        stubProfileForRead();
        when(userBadgeRepository.findDetailedByUserId(USER_ID))
                .thenReturn(List.of());

        var response =
                gamificationService.getPublicProfile(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.totalXp()).isZero();
        assertThat(response.badges()).isEmpty();
    }

    @Test
    void getPublicProfile_privateStudentProfile_hidesFullName() {
        StudentProfile studentProfile = mock(StudentProfile.class);
        when(studentProfile.getVisibility())
                .thenReturn(ProfileVisibility.PRIVATE);
        user.setStudentProfile(studentProfile);
        stubProfileForRead();
        when(userBadgeRepository.findDetailedByUserId(USER_ID))
                .thenReturn(List.of());

        var response =
                gamificationService.getPublicProfile(USER_ID);

        assertThat(response.fullName()).isNull();
    }

    @Test
    void leaderboard_allTime_returnsRankedPage() {
        LeaderboardEntryProjection projection = projection(125L);
        when(profileRepository.findAllTimeLeaderboard(any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(projection),
                        PageRequest.of(1, 10),
                        25));

        var response = gamificationService.leaderboard(
                LeaderboardPeriod.ALL_TIME,
                1,
                10);

        assertThat(response.period())
                .isEqualTo(LeaderboardPeriod.ALL_TIME);
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void leaderboard_weekly_usesKarachiWeekBoundaries() {
        when(profileRepository.findPeriodLeaderboard(
                any(Instant.class),
                any(Instant.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        gamificationService.leaderboard(
                LeaderboardPeriod.WEEKLY,
                0,
                10);

        verify(profileRepository).findPeriodLeaderboard(
                Instant.parse("2026-06-28T19:00:00Z"),
                Instant.parse("2026-07-05T19:00:00Z"),
                PageRequest.of(0, 10));
    }

    @Test
    void leaderboard_monthly_usesKarachiMonthBoundaries() {
        when(profileRepository.findPeriodLeaderboard(
                any(Instant.class),
                any(Instant.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        gamificationService.leaderboard(
                LeaderboardPeriod.MONTHLY,
                0,
                10);

        verify(profileRepository).findPeriodLeaderboard(
                Instant.parse("2026-06-30T19:00:00Z"),
                Instant.parse("2026-07-31T19:00:00Z"),
                PageRequest.of(0, 10));
    }

    @Test
    void listBadges_returnsActiveBadgesInRepositoryOrder() {
        Badge badge = badge(100);
        when(badgeRepository
                .findByActiveTrueOrderBySortOrderAscXpRequiredAsc())
                .thenReturn(List.of(badge));

        var response = gamificationService.listBadges();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().code())
                .isEqualTo("ACTIVE_STUDENT");
    }

    @Test
    void getMyBadges_returnsEarnedBadges() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        UserBadge userBadge = userBadge(badge(100));
        when(userBadgeRepository.findDetailedByUserId(USER_ID))
                .thenReturn(List.of(userBadge));

        var response = gamificationService.getMyBadges(USER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().badge().code())
                .isEqualTo("ACTIVE_STUDENT");
    }

    @Test
    void getXpHistory_returnsNewestPage() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        XpTransaction transaction = transaction();
        when(transactionRepository.findByUserId(
                eq(USER_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(transaction),
                        PageRequest.of(0, 20),
                        1));

        var response = gamificationService.getXpHistory(
                USER_ID,
                0,
                20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(TRANSACTION_ID);
    }

    @Test
    void awardXp_validAward_createsTransaction() {
        stubAward(List.of());

        var response = gamificationService.awardXp(
                USER_ID,
                GamificationActionType.NOTE_CREATED,
                25,
                GamificationSourceType.NOTE,
                SOURCE_ID,
                "Created approved study notes.");

        assertThat(response.id()).isEqualTo(TRANSACTION_ID);
        assertThat(response.points()).isEqualTo(25);
        verify(transactionRepository).save(any(XpTransaction.class));
    }

    @Test
    void awardXp_validAward_updatesTotalAndStreak() {
        stubAward(List.of());

        gamificationService.awardXp(
                USER_ID,
                GamificationActionType.NOTE_CREATED,
                25,
                GamificationSourceType.NOTE,
                SOURCE_ID,
                "Created approved study notes.");

        assertThat(profile.getTotalXp()).isEqualTo(25);
        assertThat(profile.getCurrentStreak()).isEqualTo(1);
        assertThat(profile.getLongestStreak()).isEqualTo(1);
        assertThat(profile.getLastActivityAt()).isEqualTo(NOW);
    }

    @Test
    void awardXp_crossingLevelBoundary_updatesLevel() {
        ReflectionTestUtils.setField(profile, "totalXp", 90);
        stubAward(List.of());

        gamificationService.awardXp(
                USER_ID,
                GamificationActionType.DISCUSSION_ANSWER_CREATED,
                20,
                GamificationSourceType.DISCUSSION_ANSWER,
                SOURCE_ID,
                "Answered a student question.");

        assertThat(profile.getTotalXp()).isEqualTo(110);
        assertThat(profile.getLevel()).isEqualTo(2);
    }

    @Test
    void awardXp_reachingThreshold_awardsEligibleBadge() {
        ReflectionTestUtils.setField(profile, "totalXp", 90);
        Badge eligibleBadge = badge(100);
        stubAward(List.of(eligibleBadge));
        when(userBadgeRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<UserBadge> awards = invocation.getArgument(0);
                    awards.forEach(this::setUserBadgePersistenceFields);
                    return awards;
                });

        gamificationService.awardXp(
                USER_ID,
                GamificationActionType.DISCUSSION_ANSWER_ACCEPTED,
                25,
                GamificationSourceType.DISCUSSION_ANSWER,
                SOURCE_ID,
                "Answer accepted by the question author.");

        verify(userBadgeRepository).saveAll(argThat(awards -> {
            int awardCount = 0;
            for (UserBadge ignored : awards) {
                awardCount++;
            }
            return awardCount == 1;
        }));
    }

    @Test
    void awardXp_duplicateSource_isRejectedWithoutMutation() {
        stubProfileForUpdate();
        when(transactionRepository
                .existsByUserIdAndActionTypeAndSourceTypeAndSourceId(
                        USER_ID,
                        GamificationActionType.NOTE_CREATED,
                        GamificationSourceType.NOTE,
                        SOURCE_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> gamificationService.awardXp(
                USER_ID,
                GamificationActionType.NOTE_CREATED,
                25,
                GamificationSourceType.NOTE,
                SOURCE_ID,
                "Duplicate award."))
                .isInstanceOf(GamificationConflictException.class)
                .hasMessageContaining("already been awarded");

        assertThat(profile.getTotalXp()).isZero();
        verify(transactionRepository, never())
                .save(any(XpTransaction.class));
    }

    @Test
    void awardXpIfNotAlreadyAwarded_duplicateSource_isIdempotent() {
        stubProfileForUpdate();
        when(transactionRepository
                .existsByUserIdAndActionTypeAndSourceTypeAndSourceId(
                        USER_ID,
                        GamificationActionType.NOTE_CREATED,
                        GamificationSourceType.NOTE,
                        SOURCE_ID))
                .thenReturn(true);

        var response =
                gamificationService.awardXpIfNotAlreadyAwarded(
                        USER_ID,
                        GamificationActionType.NOTE_CREATED,
                        25,
                        GamificationSourceType.NOTE,
                        SOURCE_ID,
                        "Duplicate award.");

        assertThat(response).isEmpty();
    }

    @Test
    void awardXp_nonPositivePoints_isRejected() {
        assertThatThrownBy(() -> gamificationService.awardXp(
                USER_ID,
                GamificationActionType.SYSTEM_AWARD,
                0,
                GamificationSourceType.SYSTEM,
                SOURCE_ID,
                "Invalid award."))
                .isInstanceOf(GamificationConflictException.class)
                .hasMessageContaining("greater than zero");
    }

    private void stubProfileForUpdate() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(profile));
    }

    private void stubProfileForRead() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(profileRepository.findDetailedByUserId(USER_ID))
                .thenReturn(Optional.of(profile));
    }

    private void stubAward(List<Badge> eligibleBadges) {
        stubProfileForUpdate();
        when(transactionRepository
                .existsByUserIdAndActionTypeAndSourceTypeAndSourceId(
                        any(UUID.class),
                        any(GamificationActionType.class),
                        any(GamificationSourceType.class),
                        any(UUID.class)))
                .thenReturn(false);
        when(transactionRepository.save(any(XpTransaction.class)))
                .thenAnswer(invocation -> {
                    XpTransaction saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            saved,
                            "id",
                            TRANSACTION_ID);
                    ReflectionTestUtils.setField(
                            saved,
                            "createdAt",
                            NOW);
                    return saved;
                });
        when(badgeRepository.findEligibleUnawardedBadges(
                eq(USER_ID),
                anyInt()))
                .thenReturn(eligibleBadges);
    }

    private Badge badge(int xpRequired) {
        Badge badge = new Badge(
                "ACTIVE_STUDENT",
                "Active Student",
                "Earn 100 XP",
                "XP",
                "award",
                xpRequired,
                2);
        ReflectionTestUtils.setField(badge, "id", BADGE_ID);
        return badge;
    }

    private UserBadge userBadge(Badge badge) {
        UserBadge userBadge = new UserBadge(
                badge,
                user,
                GamificationSourceType.SYSTEM,
                SOURCE_ID);
        setUserBadgePersistenceFields(userBadge);
        return userBadge;
    }

    private void setUserBadgePersistenceFields(UserBadge userBadge) {
        ReflectionTestUtils.setField(userBadge, "awardedAt", NOW);
    }

    private XpTransaction transaction() {
        XpTransaction transaction = new XpTransaction(
                user,
                GamificationActionType.NOTE_CREATED,
                25,
                GamificationSourceType.NOTE,
                SOURCE_ID,
                "Created approved study notes.");
        ReflectionTestUtils.setField(transaction, "id", TRANSACTION_ID);
        ReflectionTestUtils.setField(transaction, "createdAt", NOW);
        return transaction;
    }

    private LeaderboardEntryProjection projection(long periodXp) {
        LeaderboardEntryProjection projection =
                mock(LeaderboardEntryProjection.class);
        when(projection.getRank()).thenReturn(1L);
        when(projection.getUserId()).thenReturn(USER_ID);
        when(projection.getFullName()).thenReturn("Ayesha Malik");
        when(projection.getTotalXpForPeriod()).thenReturn(periodXp);
        when(projection.getAllTimeXp()).thenReturn(250);
        when(projection.getLevel()).thenReturn(3);
        return projection;
    }

    private void setProfilePersistenceFields(
            GamificationProfile target) {
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }
}
