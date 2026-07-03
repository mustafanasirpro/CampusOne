package com.campusone.internship.mapper;

import com.campusone.internship.dto.response.InternshipDetailResponse;
import com.campusone.internship.dto.response.InternshipPosterResponse;
import com.campusone.internship.dto.response.InternshipSummaryResponse;
import com.campusone.internship.dto.response.SavedInternshipResponse;
import com.campusone.internship.entity.Internship;
import com.campusone.internship.entity.SavedInternship;
import com.campusone.user.entity.StudentProfile;
import org.springframework.stereotype.Component;

@Component
public class InternshipMapper {

    public InternshipSummaryResponse toSummary(
            Internship internship,
            boolean savedByCurrentUser,
            boolean ownedByCurrentUser) {
        return new InternshipSummaryResponse(
                internship.getId(),
                internship.getTitle(),
                internship.getCompanyName(),
                internship.getDescription(),
                internship.getLocation(),
                internship.getInternshipType(),
                internship.getWorkMode(),
                internship.isPaid(),
                internship.getStipendAmount(),
                internship.getCurrency(),
                internship.getApplyUrl(),
                internship.getDeadline(),
                internship.getStatus(),
                toPoster(internship),
                savedByCurrentUser,
                ownedByCurrentUser,
                internship.getCreatedAt(),
                internship.getUpdatedAt());
    }

    public InternshipDetailResponse toDetail(
            Internship internship,
            boolean savedByCurrentUser,
            boolean ownedByCurrentUser) {
        return new InternshipDetailResponse(
                internship.getId(),
                internship.getTitle(),
                internship.getCompanyName(),
                internship.getDescription(),
                internship.getLocation(),
                internship.getInternshipType(),
                internship.getWorkMode(),
                internship.isPaid(),
                internship.getStipendAmount(),
                internship.getCurrency(),
                internship.getApplyUrl(),
                internship.getDeadline(),
                internship.getStatus(),
                toPoster(internship),
                savedByCurrentUser,
                ownedByCurrentUser,
                internship.getCreatedAt(),
                internship.getUpdatedAt());
    }

    public SavedInternshipResponse toSaved(SavedInternship saved) {
        return new SavedInternshipResponse(
                saved.getInternship().getId(),
                saved.getUser().getId(),
                true,
                saved.getSavedAt());
    }

    private InternshipPosterResponse toPoster(Internship internship) {
        StudentProfile profile = internship.getPoster().getStudentProfile();
        return new InternshipPosterResponse(
                internship.getPoster().getId(),
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getUniversity().getName());
    }
}
