package com.campusone.user.mapper;

import com.campusone.academic.mapper.DepartmentMapper;
import com.campusone.academic.mapper.UniversityMapper;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.dto.response.PreferenceResponse;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserMapper {

    private final UniversityMapper universityMapper;
    private final DepartmentMapper departmentMapper;

    public CurrentUserMapper(
            UniversityMapper universityMapper,
            DepartmentMapper departmentMapper) {
        this.universityMapper = universityMapper;
        this.departmentMapper = departmentMapper;
    }

    public CurrentUserResponse toResponse(
            User user,
            PreferenceResponse preferences) {
        StudentProfile profile = user.getStudentProfile();
        return new CurrentUserResponse(
                user.getId(),
                profile.getFullName(),
                user.getEmail(),
                profile.getBio(),
                universityMapper.toResponse(profile.getUniversity()),
                departmentMapper.toResponse(profile.getDepartment()),
                profile.getSemester(),
                profile.getAvatarUrl(),
                profile.getCoverImageUrl(),
                profile.getLocation(),
                profile.getSkills().stream()
                        .map(skill -> skill.getName())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList(),
                profile.getVisibility(),
                profile.getTotalXp(),
                preferences);
    }
}
