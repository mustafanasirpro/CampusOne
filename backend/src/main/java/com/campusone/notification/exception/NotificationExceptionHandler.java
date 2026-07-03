package com.campusone.notification.exception;

import com.campusone.common.dto.ErrorResponse;
import com.campusone.notification.controller.NotificationController;
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
@RestControllerAdvice(assignableTypes = NotificationController.class)
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationConflictException.class)
    ResponseEntity<ErrorResponse> handleConflict(
            NotificationConflictException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                request.getRequestId(),
                Map.of());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
