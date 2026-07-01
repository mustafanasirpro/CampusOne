package com.campusone.user.mapper;

import com.campusone.academic.mapper.DepartmentMapper;
import com.campusone.academic.mapper.UniversityMapper;
import com.campusone.user.dto.response.StudentProfileResponse;
import com.campusone.user.entity.StudentProfile;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileMapper {

    private final UniversityMapper universityMapper;
    private final DepartmentMapper departmentMapper;

    public StudentProfileMapper(
            UniversityMapper universityMapper,
            DepartmentMapper departmentMapper) {
        this.universityMapper = universityMapper;
        this.departmentMapper = departmentMapper;
    }

    public StudentProfileResponse toResponse(StudentProfile profile) {
        return new StudentProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                universityMapper.toResponse(profile.getUniversity()),
                departmentMapper.toResponse(profile.getDepartment()),
                profile.getFullName(),
                profile.getSemester(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getTotalXp());
    }
}
