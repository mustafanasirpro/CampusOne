package com.campusone.moderation.mapper;

import com.campusone.moderation.dto.response.ContentReportDetailResponse;
import com.campusone.moderation.dto.response.ContentReportSummaryResponse;
import com.campusone.moderation.dto.response.ModerationActionResponse;
import com.campusone.moderation.dto.response.ModeratorStatusResponse;
import com.campusone.moderation.dto.response.ModeratorSummaryResponse;
import com.campusone.moderation.dto.response.ReporterSummaryResponse;
import com.campusone.moderation.entity.ContentReport;
import com.campusone.moderation.entity.ModerationAction;
import com.campusone.moderation.entity.Moderator;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ModerationMapper {

    public ContentReportSummaryResponse toReportSummary(
            ContentReport report) {
        return new ContentReportSummaryResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt());
    }

    public ContentReportDetailResponse toReportDetail(
            ContentReport report) {
        return new ContentReportDetailResponse(
                report.getId(),
                toReporter(report.getReporter()),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                toModerator(report.getReviewedBy()),
                report.getReviewedAt(),
                report.getResolutionNote(),
                report.getCreatedAt(),
                report.getUpdatedAt());
    }

    public ModerationActionResponse toAction(
            ModerationAction action) {
        return new ModerationActionResponse(
                action.getId(),
                toModerator(action.getModerator()),
                action.getReport() == null
                        ? null
                        : action.getReport().getId(),
                action.getActionType(),
                action.getTargetType(),
                action.getTargetId(),
                action.getReason(),
                action.getMetadata(),
                action.getCreatedAt());
    }

    public ModeratorStatusResponse toStatus(Moderator moderator) {
        if (moderator == null || !moderator.isActive()) {
            return new ModeratorStatusResponse(
                    false,
                    null,
                    moderator == null
                            ? null
                            : moderator.getAssignedAt(),
                    moderator == null
                            ? null
                            : moderator.getRevokedAt());
        }
        return new ModeratorStatusResponse(
                true,
                moderator.getRole(),
                moderator.getAssignedAt(),
                moderator.getRevokedAt());
    }

    private ReporterSummaryResponse toReporter(User user) {
        return new ReporterSummaryResponse(
                user.getId(),
                safeFullName(user));
    }

    private ModeratorSummaryResponse toModerator(User user) {
        if (user == null) {
            return null;
        }
        return new ModeratorSummaryResponse(
                user.getId(),
                safeFullName(user));
    }

    private String safeFullName(User user) {
        StudentProfile profile = user.getStudentProfile();
        if (profile == null
                || profile.getVisibility() != ProfileVisibility.PUBLIC) {
            return null;
        }
        return profile.getFullName();
    }
}
