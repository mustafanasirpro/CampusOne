package com.campusone.ai.exception;

import com.campusone.ai.controller.AiAssistantController;
import com.campusone.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AiAssistantController.class)
public class AiExceptionHandler {

    @ExceptionHandler(AiProviderException.class)
    ResponseEntity<ErrorResponse> handleProviderFailure(
            AiProviderException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "AI_PROVIDER_FAILURE",
                exception.getMessage(),
                request.getRequestURI(),
                request.getRequestId(),
                Map.of());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
