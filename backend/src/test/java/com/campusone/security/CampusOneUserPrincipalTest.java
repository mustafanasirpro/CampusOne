package com.campusone.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.user.entity.AccountStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CampusOneUserPrincipalTest {

    @Test
    void isAccountNonLocked_futureLock_returnsFalse() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                UUID.randomUUID(),
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"),
                Instant.now().plusSeconds(300));

        assertThat(principal.isAccountNonLocked()).isFalse();
    }

    @Test
    void isAccountNonLocked_expiredLock_returnsTrue() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                UUID.randomUUID(),
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"),
                Instant.now().minusSeconds(1));

        assertThat(principal.isAccountNonLocked()).isTrue();
    }
}
