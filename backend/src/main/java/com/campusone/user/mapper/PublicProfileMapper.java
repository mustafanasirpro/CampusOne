package com.campusone.user.mapper;

import com.campusone.user.dto.response.PublicProfileResponse;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PublicProfileMapper {

    public PublicProfileResponse toResponse(User user) {
        StudentProfile profile = user.getStudentProfile();
        return new PublicProfileResponse(
                user.getId(),
                profile.getFullName(),
                profile.getBio(),
                profile.getUniversity().getName(),
                profile.getDepartment().getName(),
                profile.getSemester(),
                profile.getAvatarUrl(),
                profile.getCoverImageUrl(),
                profile.getLocation(),
                profile.getSkills().stream()
                        .map(skill -> skill.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),
                profile.getTotalXp());
    }
}
