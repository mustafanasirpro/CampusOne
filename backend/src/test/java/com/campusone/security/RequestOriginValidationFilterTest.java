package com.campusone.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.campusone.config.CorsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RequestOriginValidationFilterTest {

    @Mock
    private FilterChain filterChain;

    private RequestOriginValidationFilter filter;

    @BeforeEach
    void setUp() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://app.campusone.pk"));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new RequestOriginValidationFilter(
                properties,
                new SecurityErrorResponseWriter(objectMapper));
    }

    @Test
    void unsafeRequest_allowedFrontendOrigin_continuesChain() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.addHeader(HttpHeaders.ORIGIN, "https://app.campusone.pk");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unsafeRequest_untrustedOrigin_isRejected() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.addHeader(HttpHeaders.ORIGIN, "https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("REQUEST_ORIGIN_REJECTED");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unsafeRequest_sameSiteSiblingWithoutAllowlist_isRejected() throws Exception {
        MockHttpServletRequest request = request("POST");
        request.addHeader("Sec-Fetch-Site", "same-site");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unsafeRequest_nonBrowserClientWithoutOriginMetadata_continuesChain()
            throws Exception {
        MockHttpServletRequest request = request("POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void safeCrossSiteRequest_doesNotMutateAndContinuesChain() throws Exception {
        MockHttpServletRequest request = request("GET");
        request.addHeader("Sec-Fetch-Site", "cross-site");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/auth/refresh");
        request.setRequestURI("/api/v1/auth/refresh");
        return request;
    }
}
