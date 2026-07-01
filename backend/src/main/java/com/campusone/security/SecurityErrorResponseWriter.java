package com.campusone.security;

import com.campusone.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ErrorResponse.of(
                        status,
                        error,
                        code,
                        message,
                        request.getRequestURI(),
                        request.getRequestId()));
    }
}
