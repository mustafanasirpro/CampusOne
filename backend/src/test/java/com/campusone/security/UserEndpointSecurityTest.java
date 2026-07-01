package com.campusone.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.user.controller.UserController;
import com.campusone.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
class UserEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    @Test
    void getCurrentUser_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required to access this resource."));
    }
}
