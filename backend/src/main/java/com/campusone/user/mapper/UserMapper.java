package com.campusone.user.mapper;

import com.campusone.user.dto.response.UserResponse;
import com.campusone.user.entity.Role;
import com.campusone.user.entity.RoleName;
import com.campusone.user.entity.User;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        EnumSet<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(
                        () -> EnumSet.noneOf(RoleName.class),
                        EnumSet::add,
                        EnumSet::addAll);

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getAccountStatus(),
                user.isEmailVerified(),
                Set.copyOf(roleNames),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
