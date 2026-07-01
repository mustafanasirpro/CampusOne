package com.campusone.security;

import com.campusone.config.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestOriginValidationFilter extends OncePerRequestFilter {

    private static final String FETCH_SITE_HEADER = "Sec-Fetch-Site";
    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name());

    private final Set<String> allowedOrigins;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public RequestOriginValidationFilter(
            CorsProperties corsProperties,
            SecurityErrorResponseWriter errorResponseWriter) {
        this.allowedOrigins = Set.copyOf(corsProperties.getAllowedOrigins());
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String fetchSite = request.getHeader(FETCH_SITE_HEADER);
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if ("same-origin".equalsIgnoreCase(fetchSite)
                || (origin != null && allowedOrigins.contains(origin))) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean browserCrossOriginRequest = origin != null
                || "same-site".equalsIgnoreCase(fetchSite)
                || "cross-site".equalsIgnoreCase(fetchSite);
        if (!browserCrossOriginRequest) {
            filterChain.doFilter(request, response);
            return;
        }

        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.addHeader(HttpHeaders.VARY, FETCH_SITE_HEADER);
        errorResponseWriter.write(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "REQUEST_ORIGIN_REJECTED",
                "The request origin is not allowed.");
    }
}
