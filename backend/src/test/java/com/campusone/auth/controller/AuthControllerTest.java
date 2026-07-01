package com.campusone.auth.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.auth.dto.response.AuthResponse;
import com.campusone.auth.dto.response.UserSummaryResponse;
import com.campusone.auth.service.AuthenticationResult;
import com.campusone.auth.service.AuthService;
import com.campusone.common.exception.InvalidRefreshTokenException;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class AuthControllerTest {

    private static final String RAW_REFRESH_TOKEN = "A".repeat(43);
    private static final Instant REFRESH_EXPIRY = Instant.parse("2026-07-08T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private AuthenticationResult authenticationResult;
    private ResponseCookie refreshCookie;

    @BeforeEach
    void setUp() {
        AuthResponse response = new AuthResponse(
                "signed.jwt.token",
                900,
                new UserSummaryResponse(
                        "Ali Khan",
                        "ali.khan@example.com",
                        Set.of("STUDENT")));
        authenticationResult =
                new AuthenticationResult(response, RAW_REFRESH_TOKEN, REFRESH_EXPIRY);
        refreshCookie = ResponseCookie.from("campusone_refresh_token", RAW_REFRESH_TOKEN)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .build();
        when(refreshTokenCookieFactory.create(RAW_REFRESH_TOKEN, REFRESH_EXPIRY))
                .thenReturn(refreshCookie);
    }

    @Test
    void login_validRequest_returnsExistingBodyAndSetsRefreshCookie() throws Exception {
        when(authService.login(any())).thenReturn(authenticationResult);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ali.khan@example.com",
                                  "password": "SecurePass1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("ali.khan@example.com"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", refreshCookie.toString()));
    }

    @Test
    void refresh_validCookie_isPublicAndRotatesSession() throws Exception {
        when(refreshTokenCookieFactory.readToken(any())).thenReturn(RAW_REFRESH_TOKEN);
        when(authService.refresh(RAW_REFRESH_TOKEN)).thenReturn(authenticationResult);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie(
                                "campusone_refresh_token",
                                RAW_REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", refreshCookie.toString()));

        verify(authService).refresh(RAW_REFRESH_TOKEN);
    }

    @Test
    void logout_currentCookie_isPublicRevokesSessionAndClearsCookie() throws Exception {
        ResponseCookie cleared = ResponseCookie.from("campusone_refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
        when(refreshTokenCookieFactory.readToken(any())).thenReturn(RAW_REFRESH_TOKEN);
        when(refreshTokenCookieFactory.clear()).thenReturn(cleared);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie(
                                "campusone_refresh_token",
                                RAW_REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string(
                        "Set-Cookie",
                        allOf(
                                containsString("campusone_refresh_token="),
                                containsString("Path=/api/v1/auth"),
                                containsString("Max-Age=0"),
                                containsString("HttpOnly"),
                                containsString("SameSite=Strict"))));

        verify(authService).logout(RAW_REFRESH_TOKEN);
    }

    @Test
    void refresh_missingOrInvalidCookie_returnsStableUnauthorizedError() throws Exception {
        when(authService.refresh(null)).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"))
                .andExpect(jsonPath("$.message")
                        .value("The refresh token is missing, expired, revoked, or invalid."));
    }
}
