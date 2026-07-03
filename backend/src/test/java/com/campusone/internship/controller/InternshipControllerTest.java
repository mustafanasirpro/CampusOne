package com.campusone.internship.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.internship.dto.request.CreateInternshipRequest;
import com.campusone.internship.dto.request.InternshipSort;
import com.campusone.internship.dto.response.InternshipDetailResponse;
import com.campusone.internship.dto.response.InternshipPageResponse;
import com.campusone.internship.dto.response.InternshipPosterResponse;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import com.campusone.internship.exception.InternshipConflictException;
import com.campusone.internship.service.InternshipService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.entity.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;
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

@WebMvcTest(InternshipController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class InternshipControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID INTERNSHIP_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");
    private static final Instant DEADLINE =
            Instant.parse("2026-09-30T23:59:59Z");
    private static final Instant NOW =
            Instant.parse("2026-07-03T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternshipService internshipService;

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
    void listInternships_withoutAuthentication_isPublic() throws Exception {
        when(internshipService.listInternships(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20,
                InternshipSort.NEWEST))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/internships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getInternship_withoutAuthentication_isPublic() throws Exception {
        when(internshipService.getInternship(INTERNSHIP_ID, null))
                .thenReturn(detailResponse());

        mockMvc.perform(get(
                        "/api/v1/internships/{internshipId}",
                        INTERNSHIP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERNSHIP_ID.toString()));
    }

    @Test
    void createInternship_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(post("/api/v1/internships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myInternships_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/internships/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savedInternships_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/internships/saved"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savedState_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/internships/{internshipId}/save/me",
                        INTERNSHIP_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createInternship_withAuthentication_returnsCreated()
            throws Exception {
        when(internshipService.createInternship(
                eq(USER_ID),
                any(CreateInternshipRequest.class)))
                .thenReturn(detailResponse());

        mockMvc.perform(post("/api/v1/internships")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/internships/" + INTERNSHIP_ID))
                .andExpect(jsonPath("$.id")
                        .value(INTERNSHIP_ID.toString()));
    }

    @Test
    void createInternship_invalidRequest_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/internships")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad",
                                  "companyName": "X",
                                  "description": "Short",
                                  "location": "X",
                                  "internshipType": "SUMMER",
                                  "workMode": "HYBRID",
                                  "paid": true,
                                  "stipendAmount": -1,
                                  "currency": "X",
                                  "applyUrl": "not-a-url",
                                  "deadline": "2020-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createInternship_malformedType_returnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/v1/internships")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson().replace(
                                "\"SUMMER\"",
                                "\"APPRENTICESHIP\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void saveInternship_conflict_usesInternshipErrorEnvelope()
            throws Exception {
        when(internshipService.saveInternship(USER_ID, INTERNSHIP_ID))
                .thenThrow(new InternshipConflictException(
                        "INTERNSHIP_ALREADY_SAVED",
                        "The current user has already saved this internship."));

        mockMvc.perform(post(
                        "/api/v1/internships/{internshipId}/save",
                        INTERNSHIP_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("INTERNSHIP_ALREADY_SAVED"));
    }

    private InternshipPageResponse emptyPage() {
        return new InternshipPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private InternshipDetailResponse detailResponse() {
        return new InternshipDetailResponse(
                INTERNSHIP_ID,
                "Java Backend Intern",
                "Systems Limited",
                "Build and test production-quality Spring Boot services.",
                "Lahore",
                InternshipType.SUMMER,
                WorkMode.HYBRID,
                true,
                new BigDecimal("35000.00"),
                "PKR",
                "https://example.com/apply",
                DEADLINE,
                InternshipStatus.OPEN,
                new InternshipPosterResponse(
                        USER_ID,
                        "Ali Khan",
                        null,
                        "COMSATS University Islamabad"),
                false,
                true,
                NOW,
                NOW);
    }

    private String validCreateJson() {
        return """
                {
                  "title": "Java Backend Intern",
                  "companyName": "Systems Limited",
                  "description": "Build and test production-quality Spring Boot services.",
                  "location": "Lahore",
                  "internshipType": "SUMMER",
                  "workMode": "HYBRID",
                  "paid": true,
                  "stipendAmount": 35000.00,
                  "currency": "PKR",
                  "applyUrl": "https://example.com/apply",
                  "deadline": "2026-09-30T23:59:59Z"
                }
                """;
    }
}
