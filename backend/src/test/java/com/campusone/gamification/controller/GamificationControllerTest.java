package com.campusone.gamification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.gamification.dto.response.BadgeResponse;
import com.campusone.gamification.dto.response.GamificationProfileResponse;
import com.campusone.gamification.dto.response.LeaderboardPageResponse;
import com.campusone.gamification.dto.response.PublicGamificationProfileResponse;
import com.campusone.gamification.entity.LeaderboardPeriod;
import com.campusone.gamification.service.GamificationService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.entity.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GamificationController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class GamificationControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID BADGE_ID = UUID.fromString(
            "80000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GamificationService gamificationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "student@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
    }

    @Test
    void publicProfile_withoutAuthentication_isPublic() throws Exception {
        when(gamificationService.getPublicProfile(USER_ID))
                .thenReturn(publicProfile());

        mockMvc.perform(get(
                        "/api/v1/gamification/users/{userId}",
                        USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(USER_ID.toString()));
    }

    @Test
    void leaderboard_withoutAuthentication_isPublic() throws Exception {
        when(gamificationService.leaderboard(
                LeaderboardPeriod.ALL_TIME,
                0,
                10))
                .thenReturn(emptyLeaderboard());

        mockMvc.perform(get("/api/v1/gamification/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("ALL_TIME"))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void badges_withoutAuthentication_arePublic() throws Exception {
        when(gamificationService.listBadges())
                .thenReturn(List.of(badge()));

        mockMvc.perform(get("/api/v1/gamification/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FIRST_STEPS"));
    }

    @Test
    void protectedEndpoints_withoutAuthentication_areUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/gamification/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gamification/me/badges"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gamification/me/xp-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myProfile_withAuthentication_returnsProfile()
            throws Exception {
        when(gamificationService.getOrCreateProfile(USER_ID))
                .thenReturn(myProfile());

        mockMvc.perform(get("/api/v1/gamification/me")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(125))
                .andExpect(jsonPath("$.level").value(2));
    }

    @Test
    void leaderboard_invalidPeriod_returnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/gamification/leaderboard")
                        .param("period", "DAILY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"));
    }

    @Test
    void leaderboard_invalidPagination_returnsValidationError()
            throws Exception {
        mockMvc.perform(get("/api/v1/gamification/leaderboard")
                        .param("page", "-1")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    private GamificationProfileResponse myProfile() {
        return new GamificationProfileResponse(
                USER_ID,
                "Ayesha Malik",
                125,
                2,
                3,
                7,
                NOW,
                List.of(),
                NOW,
                NOW);
    }

    private PublicGamificationProfileResponse publicProfile() {
        return new PublicGamificationProfileResponse(
                USER_ID,
                "Ayesha Malik",
                125,
                2,
                List.of(badge()));
    }

    private LeaderboardPageResponse emptyLeaderboard() {
        return new LeaderboardPageResponse(
                LeaderboardPeriod.ALL_TIME,
                List.of(),
                0,
                10,
                0,
                0,
                true,
                true);
    }

    private BadgeResponse badge() {
        return new BadgeResponse(
                BADGE_ID,
                "FIRST_STEPS",
                "First Steps",
                "Earn your first XP on CampusOne",
                "XP",
                null,
                1,
                true,
                1);
    }
}
