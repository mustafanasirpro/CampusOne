package com.campusone.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class NoteManagementAuthorizationFilterTest {

    private static final UUID USER_ID =
            UUID.fromString("77cc499d-aa60-4a5e-941d-e0b69fd1a9ac");
    private static final String EMAIL = "student@example.com";

    private NoteAdminAuthorizationService authorizationService;
    private NoteManagementAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        authorizationService = mock(NoteAdminAuthorizationService.class);
        filter = new NoteManagementAuthorizationFilter(
                authorizationService,
                new SecurityErrorResponseWriter(
                        new ObjectMapper().findAndRegisterModules()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadSubmissionIsNotTreatedAsNoteManagement()
            throws Exception {
        authenticate();
        MockHttpServletRequest request =
                request("POST", "/api/v1/notes/upload");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void createSubmissionIsNotTreatedAsNoteManagement() throws Exception {
        authenticate();
        MockHttpServletRequest request =
                request("POST", "/api/v1/notes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void metadataUpdateRejectsAuthenticatedNormalUser() throws Exception {
        authenticate();
        when(authorizationService.canManage(USER_ID, EMAIL))
                .thenReturn(false);
        MockHttpServletRequest request =
                request("PATCH", "/api/v1/notes/77cc499d-aa60-4a5e-941d-e0b69fd1a9ac");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType())
                .startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("NOTE_ADMIN_REQUIRED")
                .contains("Only admins can edit or delete notes after submission.");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void bookmarkMutationIsNotTreatedAsNoteManagement() throws Exception {
        authenticate();
        MockHttpServletRequest request =
                request(
                        "POST",
                        "/api/v1/notes/77cc499d-aa60-4a5e-941d-e0b69fd1a9ac/bookmark");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(authorizationService);
    }

    private void authenticate() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                EMAIL,
                "password-hash",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        principal.getPassword(),
                        principal.getAuthorities()));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
