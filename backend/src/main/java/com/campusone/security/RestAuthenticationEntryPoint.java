package com.campusone.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public RestAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        Object errorCode = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE);
        String code = errorCode instanceof String value
                ? value
                : "AUTH_UNAUTHORIZED";
        Object errorMessage = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_MESSAGE);
        String message = errorMessage instanceof String value
                ? value
                : "Authentication is required to access this resource.";

        errorResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                code,
                message);
    }
}
