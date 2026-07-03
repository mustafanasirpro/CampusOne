package com.campusone.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.event.dto.request.CreateEventRequest;
import com.campusone.event.dto.request.EventSort;
import com.campusone.event.dto.response.EventDetailResponse;
import com.campusone.event.dto.response.EventOrganizerResponse;
import com.campusone.event.dto.response.EventPageResponse;
import com.campusone.event.entity.EventStatus;
import com.campusone.event.entity.EventVisibility;
import com.campusone.event.exception.EventConflictException;
import com.campusone.event.service.EventService;
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

@WebMvcTest(EventController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class EventControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant START =
            Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant END =
            Instant.parse("2026-08-15T13:00:00Z");
    private static final Instant NOW =
            Instant.parse("2026-07-03T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

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
    void listEvents_withoutAuthentication_isPublic() throws Exception {
        when(eventService.listPublicEvents(
                null,
                null,
                null,
                0,
                20,
                EventSort.UPCOMING))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getEvent_withoutAuthentication_isPublic() throws Exception {
        when(eventService.getEvent(EVENT_ID, null))
                .thenReturn(detailResponse());

        mockMvc.perform(get("/api/v1/events/{eventId}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()));
    }

    @Test
    void listMyEvents_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/events/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listJoinedEvents_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/events/joined"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void participantState_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get(
                        "/api/v1/events/{eventId}/participants/me",
                        EVENT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEvent_withAuthentication_returnsCreated() throws Exception {
        when(eventService.createEvent(
                eq(USER_ID),
                any(CreateEventRequest.class)))
                .thenReturn(detailResponse());

        mockMvc.perform(post("/api/v1/events")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/events/" + EVENT_ID))
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()));
    }

    @Test
    void createEvent_invalidTimeRange_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Campus AI Workshop",
                                  "description": "A practical workshop covering AI.",
                                  "location": "Main Auditorium",
                                  "startTime": "2026-08-15T13:00:00Z",
                                  "endTime": "2026-08-15T09:00:00Z",
                                  "capacity": 120,
                                  "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createEvent_malformedVisibility_returnsBadRequest()
            throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson().replace(
                                "\"PUBLIC\"",
                                "\"CAMPUS_ONLY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void joinEvent_conflict_usesEventErrorEnvelope() throws Exception {
        when(eventService.joinEvent(USER_ID, EVENT_ID))
                .thenThrow(new EventConflictException(
                        "EVENT_CAPACITY_FULL",
                        "The event has reached its participant capacity."));

        mockMvc.perform(post(
                        "/api/v1/events/{eventId}/participants",
                        EVENT_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_CAPACITY_FULL"));
    }

    private EventPageResponse emptyPage() {
        return new EventPageResponse(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true);
    }

    private EventDetailResponse detailResponse() {
        return new EventDetailResponse(
                EVENT_ID,
                "Campus AI Workshop",
                "A practical workshop covering responsible AI development.",
                "COMSATS Islamabad, Seminar Hall",
                START,
                END,
                120,
                0,
                EventVisibility.PUBLIC,
                EventStatus.UPCOMING,
                new EventOrganizerResponse(
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
                  "title": "Campus AI Workshop",
                  "description": "A practical workshop covering responsible AI development.",
                  "location": "COMSATS Islamabad, Seminar Hall",
                  "startTime": "2026-08-15T09:00:00Z",
                  "endTime": "2026-08-15T13:00:00Z",
                  "capacity": 120,
                  "visibility": "PUBLIC"
                }
                """;
    }
}
