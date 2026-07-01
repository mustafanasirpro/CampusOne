package com.campusone.user.mapper;

import com.campusone.academic.mapper.DepartmentMapper;
import com.campusone.academic.mapper.UniversityMapper;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserMapper {

    private final UniversityMapper universityMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;

    public CurrentUserMapper(
            UniversityMapper universityMapper,
            DepartmentMapper departmentMapper,
            RoleMapper roleMapper) {
        this.universityMapper = universityMapper;
        this.departmentMapper = departmentMapper;
        this.roleMapper = roleMapper;
    }

    public CurrentUserResponse toResponse(User user) {
        StudentProfile profile = user.getStudentProfile();
        return new CurrentUserResponse(
                user.getId(),
                profile.getFullName(),
                user.getEmail(),
                universityMapper.toResponse(profile.getUniversity()),
                departmentMapper.toResponse(profile.getDepartment()),
                profile.getSemester(),
                roleMapper.toNames(user.getRoles()));
    }
}
