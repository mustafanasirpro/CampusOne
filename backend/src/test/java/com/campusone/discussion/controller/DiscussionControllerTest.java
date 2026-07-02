package com.campusone.discussion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.discussion.dto.request.CreateQuestionRequest;
import com.campusone.discussion.dto.response.AnswerPageResponse;
import com.campusone.discussion.dto.response.DiscussionAuthorResponse;
import com.campusone.discussion.dto.response.QuestionDetailResponse;
import com.campusone.discussion.dto.response.QuestionPageResponse;
import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import com.campusone.discussion.service.DiscussionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiscussionController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class DiscussionControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID QUESTION_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID ANSWER_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscussionService discussionService;

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
    void listQuestions_withoutAuthentication_isPublic() throws Exception {
        when(discussionService.listQuestions(
                null,
                null,
                0,
                20,
                com.campusone.discussion.dto.request.QuestionSort.NEWEST))
                .thenReturn(emptyQuestionPage());

        mockMvc.perform(get("/api/v1/discussions/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getQuestion_withoutAuthentication_isPublic() throws Exception {
        when(discussionService.getQuestion(QUESTION_ID, null))
                .thenReturn(questionDetail());

        mockMvc.perform(get(
                        "/api/v1/discussions/questions/{questionId}",
                        QUESTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()));
    }

    @Test
    void listAnswers_withoutAuthentication_isPublic() throws Exception {
        when(discussionService.listAnswers(
                QUESTION_ID,
                null,
                0,
                20))
                .thenReturn(emptyAnswerPage());

        mockMvc.perform(get(
                        "/api/v1/discussions/questions/{questionId}/answers",
                        QUESTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listMyQuestions_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/discussions/questions/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuestion_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(post("/api/v1/discussions/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQuestionJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuestion_withAuthentication_returnsCreated()
            throws Exception {
        when(discussionService.createQuestion(
                eq(USER_ID),
                any(CreateQuestionRequest.class)))
                .thenReturn(questionDetail());

        mockMvc.perform(post("/api/v1/discussions/questions")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQuestionJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/discussions/questions/" + QUESTION_ID))
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()));
    }

    @Test
    void voteQuestion_zeroVote_returnsValidationError() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/discussions/questions/{questionId}/vote",
                        QUESTION_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteValue\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void voteAnswer_malformedVote_returnsBadRequest() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/discussions/answers/{answerId}/vote",
                        ANSWER_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteValue\":\"up\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private QuestionPageResponse emptyQuestionPage() {
        return new QuestionPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private AnswerPageResponse emptyAnswerPage() {
        return new AnswerPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private QuestionDetailResponse questionDetail() {
        return new QuestionDetailResponse(
                QUESTION_ID,
                "How does dependency injection work?",
                "I understand the basic idea but need a practical explanation.",
                DiscussionCategory.PROGRAMMING,
                DiscussionQuestionStatus.OPEN,
                new DiscussionAuthorResponse(
                        USER_ID,
                        "Ali Khan",
                        null,
                        "COMSATS University Islamabad"),
                0,
                0,
                0,
                null,
                null,
                true,
                emptyAnswerPage(),
                NOW,
                NOW);
    }

    private String validQuestionJson() {
        return """
                {
                  "title": "How does dependency injection work?",
                  "body": "I understand the basic idea but need a practical explanation.",
                  "category": "PROGRAMMING"
                }
                """;
    }
}
