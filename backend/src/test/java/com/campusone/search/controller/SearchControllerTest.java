package com.campusone.search.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.dto.response.GlobalSearchResponse;
import com.campusone.search.dto.response.SearchSuggestionResponse;
import com.campusone.search.dto.response.SearchTypeResponse;
import com.campusone.search.service.GlobalSearchService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GlobalSearchService searchService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    @Test
    void search_withoutAuthentication_isPublic() throws Exception {
        when(searchService.search(
                "java",
                null,
                0,
                10,
                SearchSort.RELEVANCE,
                null))
                .thenReturn(emptySearchResponse());

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("java"))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void search_commaSeparatedTypes_areConverted() throws Exception {
        when(searchService.search(
                eq("java"),
                eq(Set.of(SearchType.NOTE, SearchType.EVENT)),
                eq(0),
                eq(10),
                eq(SearchSort.RELEVANCE),
                eq(null)))
                .thenReturn(emptySearchResponse());

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "java")
                        .param("types", "NOTE,EVENT"))
                .andExpect(status().isOk());
    }

    @Test
    void search_blankQuery_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void search_shortTrimmedQuery_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", " a "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void search_invalidType_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "java")
                        .param("types", "NOTE,ARTICLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"));
    }

    @Test
    void search_invalidSort_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "java")
                        .param("sort", "POPULAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_REQUEST"));
    }

    @Test
    void suggestions_withoutAuthentication_isPublic() throws Exception {
        when(searchService.suggestions("java", 5, null))
                .thenReturn(new SearchSuggestionResponse(
                        "java",
                        List.of("Java OOP Notes")));

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]")
                        .value("Java OOP Notes"));
    }

    @Test
    void suggestions_invalidQueryAndLimit_areRejected()
            throws Exception {
        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("q", "a")
                        .param("limit", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"));
    }

    @Test
    void types_withoutAuthentication_returnsSupportedTypes()
            throws Exception {
        when(searchService.types()).thenReturn(List.of(
                new SearchTypeResponse(
                        SearchType.NOTE,
                        "Notes",
                        "Approved public notes.")));

        mockMvc.perform(get("/api/v1/search/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NOTE"))
                .andExpect(jsonPath("$[0].displayName").value("Notes"));
    }

    private GlobalSearchResponse emptySearchResponse() {
        return new GlobalSearchResponse(
                "java",
                0,
                10,
                0,
                0,
                false,
                List.of());
    }
}
