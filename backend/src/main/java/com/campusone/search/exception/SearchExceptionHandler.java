package com.campusone.search.exception;

import com.campusone.common.dto.ErrorResponse;
import com.campusone.search.controller.SearchController;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SearchController.class)
public class SearchExceptionHandler {

    @ExceptionHandler(SearchValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            SearchValidationException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request.getRequestURI(),
                request.getRequestId(),
                Map.of(
                        exception.getField(),
                        List.of(exception.getMessage())));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
