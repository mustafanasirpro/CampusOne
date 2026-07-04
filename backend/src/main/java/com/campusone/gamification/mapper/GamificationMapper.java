package com.campusone.gamification.mapper;

import com.campusone.gamification.dto.response.BadgeResponse;
import com.campusone.gamification.dto.response.GamificationProfileResponse;
import com.campusone.gamification.dto.response.LeaderboardEntryResponse;
import com.campusone.gamification.dto.response.PublicGamificationProfileResponse;
import com.campusone.gamification.dto.response.UserBadgeResponse;
import com.campusone.gamification.dto.response.XpTransactionResponse;
import com.campusone.gamification.entity.Badge;
import com.campusone.gamification.entity.GamificationProfile;
import com.campusone.gamification.entity.UserBadge;
import com.campusone.gamification.entity.XpTransaction;
import com.campusone.gamification.repository.LeaderboardEntryProjection;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.StudentProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GamificationMapper {

    public GamificationProfileResponse toProfile(
            GamificationProfile profile,
            List<UserBadge> userBadges) {
        return new GamificationProfileResponse(
                profile.getUserId(),
                fullName(profile, false),
                profile.getTotalXp(),
                profile.getLevel(),
                profile.getCurrentStreak(),
                profile.getLongestStreak(),
                profile.getLastActivityAt(),
                userBadges.stream().map(this::toUserBadge).toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    public PublicGamificationProfileResponse toPublicProfile(
            GamificationProfile profile,
            List<UserBadge> userBadges) {
        return new PublicGamificationProfileResponse(
                profile.getUserId(),
                fullName(profile, true),
                profile.getTotalXp(),
                profile.getLevel(),
                userBadges.stream()
                        .map(UserBadge::getBadge)
                        .map(this::toBadge)
                        .toList());
    }

    public BadgeResponse toBadge(Badge badge) {
        return new BadgeResponse(
                badge.getId(),
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getCategory(),
                badge.getIcon(),
                badge.getXpRequired(),
                badge.isActive(),
                badge.getSortOrder());
    }

    public UserBadgeResponse toUserBadge(UserBadge userBadge) {
        return new UserBadgeResponse(
                toBadge(userBadge.getBadge()),
                userBadge.getAwardedAt(),
                userBadge.getSourceType(),
                userBadge.getSourceId());
    }

    public XpTransactionResponse toTransaction(
            XpTransaction transaction) {
        return new XpTransactionResponse(
                transaction.getId(),
                transaction.getActionType(),
                transaction.getPoints(),
                transaction.getSourceType(),
                transaction.getSourceId(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }

    public LeaderboardEntryResponse toLeaderboardEntry(
            LeaderboardEntryProjection projection) {
        return new LeaderboardEntryResponse(
                projection.getRank(),
                projection.getUserId(),
                projection.getFullName(),
                projection.getTotalXpForPeriod(),
                projection.getAllTimeXp(),
                projection.getLevel());
    }

    private String fullName(
            GamificationProfile profile,
            boolean respectPrivacy) {
        StudentProfile studentProfile =
                profile.getUser().getStudentProfile();
        if (studentProfile == null
                || (respectPrivacy
                    && studentProfile.getVisibility()
                        == ProfileVisibility.PRIVATE)) {
            return null;
        }
        return studentProfile.getFullName();
    }
}
