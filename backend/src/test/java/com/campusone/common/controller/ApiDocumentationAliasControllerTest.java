package com.campusone.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiDocumentationAliasControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ApiDocumentationAliasController())
                .build();
    }

    @Test
    void legacyOpenApiRoutes_redirectToConfiguredDocument()
            throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/v1/openapi"));
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/api/v1/openapi/swagger-config"));
    }

    @Test
    void legacySwaggerRoutes_redirectToConfiguredUi()
            throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/v1/swagger-ui"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/v1/swagger-ui"));
    }
}
