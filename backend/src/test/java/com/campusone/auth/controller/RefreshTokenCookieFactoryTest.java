package com.campusone.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.config.AuthSessionProperties;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

class RefreshTokenCookieFactoryTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    private RefreshTokenCookieFactory cookieFactory;

    @BeforeEach
    void setUp() {
        AuthSessionProperties properties = new AuthSessionProperties();
        properties.setCookieSecure(true);
        properties.setCookieSameSite("Strict");
        cookieFactory = new RefreshTokenCookieFactory(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void create_validToken_returnsHardenedAuthScopedCookie() {
        ResponseCookie cookie = cookieFactory.create(
                "A".repeat(43),
                NOW.plus(Duration.ofDays(7)));

        assertThat(cookie.getName()).isEqualTo("campusone_refresh_token");
        assertThat(cookie.getValue()).isEqualTo("A".repeat(43));
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void clear_whenLoggingOut_returnsImmediatelyExpiredCookie() {
        ResponseCookie cookie = cookieFactory.clear();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void readToken_matchingCookie_returnsOpaqueToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("unrelated", "value"),
                new Cookie("campusone_refresh_token", "A".repeat(43)));

        assertThat(cookieFactory.readToken(request)).isEqualTo("A".repeat(43));
    }

    @Test
    void readToken_noCookies_returnsNull() {
        assertThat(cookieFactory.readToken(new MockHttpServletRequest())).isNull();
    }
}
