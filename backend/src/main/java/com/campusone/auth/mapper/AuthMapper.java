package com.campusone.auth.mapper;

import com.campusone.auth.dto.response.UserSummaryResponse;
import com.campusone.user.entity.User;
import com.campusone.user.mapper.RoleMapper;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    private final RoleMapper roleMapper;

    public AuthMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public UserSummaryResponse toUserSummary(User user) {
        return new UserSummaryResponse(
                user.getStudentProfile().getFullName(),
                user.getEmail(),
                roleMapper.toNames(user.getRoles()));
    }
}
