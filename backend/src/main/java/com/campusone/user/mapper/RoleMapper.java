package com.campusone.user.mapper;

import com.campusone.user.entity.Role;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public Set<String> toNames(Set<Role> roles) {
        Set<String> names = roles.stream()
                .map(role -> role.getName().name())
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(names);
    }
}
