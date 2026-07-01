package com.campusone.user.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.dto.response.PublicProfileResponse;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.service.CurrentUserService;
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

@WebMvcTest(ProfileController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class ProfileControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private PublicProfileResponse response;

    @BeforeEach
    void setUp() {
        response = new PublicProfileResponse(
                USER_ID,
                "Ali Khan",
                "Computer Science student",
                "COMSATS University Islamabad",
                "Computer Science",
                4,
                "https://cdn.example.com/avatar.png",
                "https://cdn.example.com/cover.png",
                "Islamabad",
                List.of("Java", "React"),
                250);
    }

    @Test
    void getProfile_publicProfile_isAvailableWithoutAuthentication() throws Exception {
        when(currentUserService.getPublicProfile(USER_ID, null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/profiles/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.fullName").value("Ali Khan"))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.preferences").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist());

        verify(currentUserService).getPublicProfile(USER_ID, null);
    }

    @Test
    void getProfile_authenticatedRequest_passesViewerIdentityToVisibilityCheck()
            throws Exception {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities());
        when(currentUserService.getPublicProfile(USER_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/profiles/{userId}", USER_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isOk());

        verify(currentUserService).getPublicProfile(USER_ID, USER_ID);
    }

    @Test
    void getProfile_malformedUserId_returnsControlledBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
