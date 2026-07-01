package com.campusone.security;

import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.User;
import java.io.Serial;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CampusOneUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final AccountStatus accountStatus;
    private final Set<String> roleNames;
    private final List<GrantedAuthority> authorities;

    public CampusOneUserPrincipal(
            UUID userId,
            String email,
            String passwordHash,
            AccountStatus accountStatus,
            Set<String> roleNames) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.roleNames = roleNames.stream()
                .sorted()
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.authorities = this.roleNames.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .sorted(Comparator.comparing(GrantedAuthority::getAuthority))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static CampusOneUserPrincipal from(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new CampusOneUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getAccountStatus(),
                roles);
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<String> getRoleNames() {
        return new LinkedHashSet<>(roleNames);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
