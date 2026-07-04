package com.campusone.gamification.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.gamification.dto.response.BadgeResponse;
import com.campusone.gamification.dto.response.GamificationProfileResponse;
import com.campusone.gamification.dto.response.LeaderboardEntryResponse;
import com.campusone.gamification.dto.response.LeaderboardPageResponse;
import com.campusone.gamification.dto.response.PublicGamificationProfileResponse;
import com.campusone.gamification.dto.response.UserBadgeResponse;
import com.campusone.gamification.dto.response.XpHistoryPageResponse;
import com.campusone.gamification.dto.response.XpTransactionResponse;
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
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationService {

    private static final ZoneId LEADERBOARD_ZONE =
            ZoneId.of("Asia/Karachi");

    private final GamificationProfileRepository profileRepository;
    private final XpTransactionRepository transactionRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final GamificationMapper mapper;
    private final Clock clock;

    public GamificationService(
            GamificationProfileRepository profileRepository,
            XpTransactionRepository transactionRepository,
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            UserRepository userRepository,
            GamificationMapper mapper,
            Clock clock) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public GamificationProfileResponse getOrCreateProfile(UUID userId) {
        GamificationProfile profile = profileForReadOrCreate(userId);
        List<UserBadge> badges =
                userBadgeRepository.findDetailedByUserId(userId);
        return mapper.toProfile(profile, badges);
    }

    @Transactional
    public PublicGamificationProfileResponse getPublicProfile(UUID userId) {
        GamificationProfile profile = profileForReadOrCreate(userId);
        List<UserBadge> badges =
                userBadgeRepository.findDetailedByUserId(userId);
        return mapper.toPublicProfile(profile, badges);
    }

    @Transactional(readOnly = true)
    public LeaderboardPageResponse leaderboard(
            LeaderboardPeriod period,
            int page,
            int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<LeaderboardEntryProjection> leaderboard = switch (period) {
            case ALL_TIME ->
                    profileRepository.findAllTimeLeaderboard(pageRequest);
            case WEEKLY, MONTHLY -> {
                PeriodWindow window = periodWindow(period);
                yield profileRepository.findPeriodLeaderboard(
                        window.start(),
                        window.end(),
                        pageRequest);
            }
        };
        List<LeaderboardEntryResponse> content =
                leaderboard.getContent().stream()
                        .map(mapper::toLeaderboardEntry)
                        .toList();
        return new LeaderboardPageResponse(
                period,
                content,
                leaderboard.getNumber(),
                leaderboard.getSize(),
                leaderboard.getTotalElements(),
                leaderboard.getTotalPages(),
                leaderboard.isFirst(),
                leaderboard.isLast());
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> listBadges() {
        return badgeRepository
                .findByActiveTrueOrderBySortOrderAscXpRequiredAsc()
                .stream()
                .map(mapper::toBadge)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getMyBadges(UUID userId) {
        requireUser(userId);
        return userBadgeRepository.findDetailedByUserId(userId)
                .stream()
                .map(mapper::toUserBadge)
                .toList();
    }

    @Transactional(readOnly = true)
    public XpHistoryPageResponse getXpHistory(
            UUID userId,
            int page,
            int size) {
        requireUser(userId);
        Page<XpTransaction> transactions =
                transactionRepository.findByUserId(
                        userId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.asc("id"))));
        return new XpHistoryPageResponse(
                transactions.getContent().stream()
                        .map(mapper::toTransaction)
                        .toList(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages(),
                transactions.isFirst(),
                transactions.isLast());
    }

    @Transactional
    public XpTransactionResponse awardXp(
            UUID userId,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description) {
        return award(
                userId,
                actionType,
                points,
                sourceType,
                sourceId,
                description,
                true).orElseThrow();
    }

    @Transactional
    public Optional<XpTransactionResponse> awardXpIfNotAlreadyAwarded(
            UUID userId,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description) {
        return award(
                userId,
                actionType,
                points,
                sourceType,
                sourceId,
                description,
                false);
    }

    @Transactional
    public List<UserBadgeResponse> evaluateAndAwardBadges(UUID userId) {
        GamificationProfile profile = profileForUpdate(userId);
        return evaluateAndAwardBadges(
                profile,
                null,
                null).stream()
                .map(mapper::toUserBadge)
                .toList();
    }

    public int recalculateLevel(int totalXp) {
        if (totalXp < 0) {
            throw invalidAward("Total XP cannot be negative.");
        }
        return totalXp / 100 + 1;
    }

    private Optional<XpTransactionResponse> award(
            UUID userId,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description,
            boolean rejectDuplicate) {
        validateAward(
                userId,
                actionType,
                points,
                sourceType,
                sourceId,
                description);
        GamificationProfile profile = profileForUpdate(userId);
        if (hasDuplicateKey(sourceType, sourceId)
                && transactionRepository
                        .existsByUserIdAndActionTypeAndSourceTypeAndSourceId(
                                userId,
                                actionType,
                                sourceType,
                                sourceId)) {
            if (rejectDuplicate) {
                throw new GamificationConflictException(
                        "XP_AWARD_DUPLICATE",
                        "XP has already been awarded for this source.",
                        HttpStatus.CONFLICT);
            }
            return Optional.empty();
        }

        int updatedTotal;
        try {
            updatedTotal = Math.addExact(
                    profile.getTotalXp(),
                    points);
        } catch (ArithmeticException exception) {
            throw invalidAward("XP total exceeds the supported range.");
        }
        Instant awardedAt = clock.instant();
        profile.awardXp(
                points,
                recalculateLevel(updatedTotal),
                awardedAt);
        XpTransaction transaction = transactionRepository.save(
                new XpTransaction(
                        profile.getUser(),
                        actionType,
                        points,
                        sourceType,
                        sourceId,
                        description));
        evaluateAndAwardBadges(
                profile,
                sourceType,
                sourceId);
        return Optional.of(mapper.toTransaction(transaction));
    }

    private List<UserBadge> evaluateAndAwardBadges(
            GamificationProfile profile,
            GamificationSourceType sourceType,
            UUID sourceId) {
        List<Badge> eligibleBadges =
                badgeRepository.findEligibleUnawardedBadges(
                        profile.getUserId(),
                        profile.getTotalXp());
        if (eligibleBadges.isEmpty()) {
            return List.of();
        }
        List<UserBadge> awards = eligibleBadges.stream()
                .map(badge -> new UserBadge(
                        badge,
                        profile.getUser(),
                        sourceType,
                        sourceId))
                .toList();
        return userBadgeRepository.saveAll(awards);
    }

    private GamificationProfile profileForUpdate(UUID userId) {
        requireUser(userId);
        profileRepository.insertIfMissing(userId);
        return profileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gamification profile"));
    }

    private GamificationProfile profileForReadOrCreate(UUID userId) {
        requireUser(userId);
        profileRepository.insertIfMissing(userId);
        return profileRepository.findDetailedByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Gamification profile"));
    }

    private User requireUser(UUID userId) {
        if (userId == null) {
            throw new ResourceNotFoundException("User");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private void validateAward(
            UUID userId,
            GamificationActionType actionType,
            int points,
            GamificationSourceType sourceType,
            UUID sourceId,
            String description) {
        if (userId == null) {
            throw invalidAward("A user ID is required.");
        }
        if (actionType == null) {
            throw invalidAward("An XP action type is required.");
        }
        if (points <= 0) {
            throw invalidAward("XP points must be greater than zero.");
        }
        if (sourceId != null && sourceType == null) {
            throw invalidAward(
                    "A source type is required when a source ID is provided.");
        }
        if (description != null
                && description.trim().length() > 500) {
            throw invalidAward(
                    "XP description cannot exceed 500 characters.");
        }
    }

    private boolean hasDuplicateKey(
            GamificationSourceType sourceType,
            UUID sourceId) {
        return sourceType != null && sourceId != null;
    }

    private PeriodWindow periodWindow(LeaderboardPeriod period) {
        ZonedDateTime now = clock.instant().atZone(LEADERBOARD_ZONE);
        return switch (period) {
            case WEEKLY -> {
                ZonedDateTime start = now.toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(
                                DayOfWeek.MONDAY))
                        .atStartOfDay(LEADERBOARD_ZONE);
                yield new PeriodWindow(
                        start.toInstant(),
                        start.plusWeeks(1).toInstant());
            }
            case MONTHLY -> {
                ZonedDateTime start = now.toLocalDate()
                        .withDayOfMonth(1)
                        .atStartOfDay(LEADERBOARD_ZONE);
                yield new PeriodWindow(
                        start.toInstant(),
                        start.plusMonths(1).toInstant());
            }
            case ALL_TIME -> throw new IllegalArgumentException(
                    "ALL_TIME does not use a period window.");
        };
    }

    private GamificationConflictException invalidAward(String message) {
        return new GamificationConflictException(
                "XP_AWARD_INVALID",
                message,
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private record PeriodWindow(Instant start, Instant end) {
    }
}
