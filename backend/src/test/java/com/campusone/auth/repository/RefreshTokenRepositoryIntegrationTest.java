package com.campusone.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.auth.entity.RefreshToken;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.Role;
import com.campusone.user.entity.RoleName;
import com.campusone.user.entity.User;
import com.campusone.user.repository.RoleRepository;
import com.campusone.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.security.servlet."
            + "UserDetailsServiceAutoConfiguration",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Transactional
class RefreshTokenRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findByTokenHashForUpdate_persistedHash_returnsSessionCreatedByFlywaySchema() {
        Role student = roleRepository.findByName(RoleName.STUDENT).orElseThrow();
        User user = new User(
                "refresh.integration@example.com",
                "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.addRole(student);
        user = userRepository.saveAndFlush(user);

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String tokenHash = "a".repeat(64);
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                tokenHash,
                UUID.randomUUID(),
                now.plus(7, ChronoUnit.DAYS),
                now));

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow();

        assertThat(stored.getTokenHash()).isEqualTo(tokenHash);
        assertThat(stored.getUser().getEmail())
                .isEqualTo("refresh.integration@example.com");
        assertThat(stored.getUser().getRoles())
                .extracting(Role::getName)
                .containsExactly(RoleName.STUDENT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class,
                tokenHash))
                .isEqualTo(1);
    }
}
