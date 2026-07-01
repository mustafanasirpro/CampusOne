package com.campusone.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.academic.dto.response.DepartmentResponse;
import com.campusone.academic.dto.response.UniversityResponse;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.dto.request.UpdateProfileRequest;
import com.campusone.user.dto.request.UpdateSkillsRequest;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.dto.response.PreferenceResponse;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.ThemePreference;
import com.campusone.user.service.CurrentUserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");
    private static final UUID UNIVERSITY_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private UsernamePasswordAuthenticationToken authentication;
    private CurrentUserResponse response;

    @BeforeEach
    void setUp() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
        response = currentUserResponse();
    }

    @Test
    void getCurrentUser_authenticatedOwner_returnsProfile() throws Exception {
        when(currentUserService.getCurrentUser(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.fullName").value("Ali Khan"))
                .andExpect(jsonPath("$.email").value("ali.khan@example.com"))
                .andExpect(jsonPath("$.roles").doesNotExist())
                .andExpect(jsonPath("$.preferences.theme").value("SYSTEM"));

        verify(currentUserService).getCurrentUser(USER_ID);
    }

    @Test
    void updateCurrentUser_validOwnerRequest_returnsUpdatedProfile() throws Exception {
        when(currentUserService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Building accessible student tools.",
                                  "semester": 5,
                                  "visibility": "PUBLIC",
                                  "preferences": {
                                    "theme": "DARK",
                                    "language": "en-PK",
                                    "compactMode": true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ali Khan"));

        verify(currentUserService)
                .updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
    }

    @Test
    void updateCurrentUser_invalidFields_rejectsBeforeService() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semester": 9,
                                  "avatarUrl": "javascript:alert(1)"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.semester").exists())
                .andExpect(jsonPath("$.fieldErrors.avatarUrl").exists());

        verify(currentUserService, never())
                .updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
    }

    @Test
    void replaceSkills_validOwnerRequest_returnsUpdatedProfile() throws Exception {
        when(currentUserService.replaceSkills(eq(USER_ID), any(UpdateSkillsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me/skills")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"React\",\"Java\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.skills[1]").value("React"));

        verify(currentUserService)
                .replaceSkills(eq(USER_ID), any(UpdateSkillsRequest.class));
    }

    private CurrentUserResponse currentUserResponse() {
        return new CurrentUserResponse(
                USER_ID,
                "Ali Khan",
                "ali.khan@example.com",
                "Computer Science student",
                new UniversityResponse(
                        UNIVERSITY_ID,
                        "COMSATS University Islamabad",
                        "COMSATS",
                        "Islamabad",
                        "https://www.comsats.edu.pk",
                        true),
                new DepartmentResponse(
                        DEPARTMENT_ID,
                        UNIVERSITY_ID,
                        "Computer Science",
                        "CS",
                        true),
                4,
                "https://cdn.example.com/avatar.png",
                "https://cdn.example.com/cover.png",
                "Islamabad",
                List.of("Java", "React"),
                ProfileVisibility.PUBLIC,
                250,
                new PreferenceResponse(ThemePreference.SYSTEM, "en", false));
    }
}
