package com.campusone.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusone.user.entity.AccountStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-07-01T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000001");

    private JwtProperties properties;
    private CampusOneUserPrincipal principal;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(UTF_8)));
        properties.setIssuer("campusone-backend-test");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        principal = new CampusOneUserPrincipal(
                USER_ID,
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
    }

    @Test
    void parseAccessToken_validToken_returnsRequiredClaims() {
        JwtService jwtService = serviceAt(ISSUED_AT);

        String token = jwtService.generateAccessToken(principal);
        AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.email()).isEqualTo("ali.khan@example.com");
        assertThat(claims.roles()).containsExactly("STUDENT");
        assertThat(claims.expiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofMinutes(15)));
        assertThat(jwtService.isAccessTokenValid(claims, principal)).isTrue();
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    void parseAccessToken_invalidSignature_rejectsToken() {
        JwtService jwtService = serviceAt(ISSUED_AT);
        String token = jwtService.generateAccessToken(principal);
        String tamperedToken = token.substring(0, token.length() - 2) + "aa";

        assertThatThrownBy(() -> jwtService.parseAccessToken(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseAccessToken_expiredToken_rejectsToken() {
        String token = serviceAt(ISSUED_AT).generateAccessToken(principal);
        JwtService serviceAfterExpiry = serviceAt(ISSUED_AT.plus(Duration.ofMinutes(16)));

        assertThatThrownBy(() -> serviceAfterExpiry.parseAccessToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private JwtService serviceAt(Instant instant) {
        return new JwtService(
                properties,
                Clock.fixed(instant, ZoneOffset.UTC));
    }
}
