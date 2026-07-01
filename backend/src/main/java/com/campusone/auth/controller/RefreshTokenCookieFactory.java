package com.campusone.auth.controller;

import com.campusone.config.AuthSessionProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Strict";

    private final AuthSessionProperties properties;
    private final Clock clock;

    public RefreshTokenCookieFactory(
            AuthSessionProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public ResponseCookie create(String token, Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        if (maxAge.isNegative()) {
            maxAge = Duration.ZERO;
        }
        return baseCookie(token)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.getRefreshTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie
                .from(properties.getRefreshTokenCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH);
    }
}
