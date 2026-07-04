package com.campusone.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.ai.dto.request.CreateAiSessionRequest;
import com.campusone.ai.dto.request.SendAiMessageRequest;
import com.campusone.ai.dto.response.AiChatResponse;
import com.campusone.ai.dto.response.AiMessageResponse;
import com.campusone.ai.dto.response.AiSessionSummaryResponse;
import com.campusone.ai.entity.AiMessageRole;
import com.campusone.ai.entity.AiSessionMode;
import com.campusone.ai.provider.LocalStudyAiProvider;
import com.campusone.ai.service.AiAssistantService;
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

@WebMvcTest(AiAssistantController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class AiAssistantControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString(
            "a1000000-0000-4000-8000-000000000001");
    private static final UUID ITEM_ID = UUID.fromString(
            "a2000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAssistantService aiAssistantService;

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
    void allAiEndpoints_withoutAuthentication_areUnauthorized()
            throws Exception {
        assertUnauthorized(post("/api/v1/ai/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(get("/api/v1/ai/sessions"));
        assertUnauthorized(get(
                "/api/v1/ai/sessions/{sessionId}",
                SESSION_ID));
        assertUnauthorized(delete(
                "/api/v1/ai/sessions/{sessionId}",
                SESSION_ID));
        assertUnauthorized(post(
                        "/api/v1/ai/sessions/{sessionId}/messages",
                        SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(get(
                "/api/v1/ai/sessions/{sessionId}/messages",
                SESSION_ID));
        assertUnauthorized(post("/api/v1/ai/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(post("/api/v1/ai/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(post("/api/v1/ai/flashcards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(post("/api/v1/ai/quiz")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(post("/api/v1/ai/study-plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"));
        assertUnauthorized(get("/api/v1/ai/generated-items"));
        assertUnauthorized(get(
                "/api/v1/ai/generated-items/{itemId}",
                ITEM_ID));
        assertUnauthorized(delete(
                "/api/v1/ai/generated-items/{itemId}",
                ITEM_ID));
        assertUnauthorized(get("/api/v1/ai/usage"));
    }

    @Test
    void createSession_validRequest_returnsCreatedSession()
            throws Exception {
        when(aiAssistantService.createSession(
                eq(USER_ID),
                any(CreateAiSessionRequest.class)))
                .thenReturn(new AiSessionSummaryResponse(
                        SESSION_ID,
                        "Binary tree help",
                        AiSessionMode.GENERAL_CHAT,
                        NOW,
                        NOW));

        mockMvc.perform(post("/api/v1/ai/sessions")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Binary tree help",
                                  "mode": "GENERAL_CHAT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/ai/sessions/" + SESSION_ID))
                .andExpect(jsonPath("$.id")
                        .value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.mode")
                        .value("GENERAL_CHAT"));
    }

    @Test
    void sendMessage_validRequest_returnsLocalProviderResponse()
            throws Exception {
        AiMessageResponse userMessage = new AiMessageResponse(
                UUID.randomUUID(),
                AiMessageRole.USER,
                "Explain binary trees",
                5,
                NOW);
        AiMessageResponse assistantMessage = new AiMessageResponse(
                UUID.randomUUID(),
                AiMessageRole.ASSISTANT,
                "Let us break this down step by step.",
                9,
                NOW);
        when(aiAssistantService.sendMessage(
                eq(USER_ID),
                eq(SESSION_ID),
                any(SendAiMessageRequest.class)))
                .thenReturn(new AiChatResponse(
                        SESSION_ID,
                        userMessage,
                        assistantMessage,
                        LocalStudyAiProvider.PROVIDER_NAME));

        mockMvc.perform(post(
                        "/api/v1/ai/sessions/{sessionId}/messages",
                        SESSION_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Explain binary trees"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage.role")
                        .value("ASSISTANT"))
                .andExpect(jsonPath("$.provider")
                        .value(LocalStudyAiProvider.PROVIDER_NAME));
    }

    @Test
    void generation_invalidRequest_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/ai/summarize")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "x", "text": "too short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void listSessions_invalidPagination_returnsValidationError()
            throws Exception {
        mockMvc.perform(get("/api/v1/ai/sessions")
                        .with(authentication(authentication))
                        .param("page", "-1")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void listSessions_invalidSort_returnsMalformedRequest()
            throws Exception {
        mockMvc.perform(get("/api/v1/ai/sessions")
                        .with(authentication(authentication))
                        .param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"));
    }

    private void assertUnauthorized(
            org.springframework.test.web.servlet.RequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
}
