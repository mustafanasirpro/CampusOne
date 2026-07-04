package com.campusone.gamification.exception;

import com.campusone.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GamificationExceptionHandler {

    @ExceptionHandler(GamificationConflictException.class)
    ResponseEntity<ErrorResponse> handleConflict(
            GamificationConflictException exception,
            HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                exception.getStatus().value(),
                exception.getStatus().getReasonPhrase(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                request.getRequestId(),
                Map.of());
        return ResponseEntity.status(exception.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
