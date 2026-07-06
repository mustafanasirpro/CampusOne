package com.campusone.common.exception;

import com.campusone.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${app.storage.max-upload-size-mb:25}")
    private int maximumUploadSizeMb = 25;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>())
                        .add(fieldError.getDefaultMessage()));

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "The request body is malformed or unreadable.",
                request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleMalformedPathParameter(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "A path or query parameter has an invalid value.",
                request,
                Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "The request content type is not supported.",
                request,
                Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.computeIfAbsent(
                                violation.getPropertyPath().toString(),
                                ignored -> new ArrayList<>())
                        .add(violation.getMessage()));

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "AUTH_EMAIL_ALREADY_EXISTS",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found.",
                request,
                Map.of());
    }

    @ExceptionHandler(InvalidAcademicSelectionException.class)
    ResponseEntity<ErrorResponse> handleInvalidAcademicSelection(
            InvalidAcademicSelectionException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_ACADEMIC_SELECTION",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(InvalidNoteStateException.class)
    ResponseEntity<ErrorResponse> handleInvalidNoteState(
            InvalidNoteStateException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "INVALID_NOTE_STATE",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(StorageNotConfiguredException.class)
    ResponseEntity<ErrorResponse> handleStorageNotConfigured(
            StorageNotConfiguredException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "STORAGE_NOT_CONFIGURED",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(InvalidFileUploadException.class)
    ResponseEntity<ErrorResponse> handleInvalidFileUpload(
            InvalidFileUploadException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_FILE_UPLOAD",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(FileUploadTooLargeException.class)
    ResponseEntity<ErrorResponse> handleFileUploadTooLarge(
            FileUploadTooLargeException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleMultipartSizeLimit(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "File size must be " + maximumUploadSizeMb + " MB or less.",
                request,
                Map.of());
    }

    @ExceptionHandler(UploadLimitExceededException.class)
    ResponseEntity<ErrorResponse> handleUploadLimitExceeded(
            UploadLimitExceededException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.TOO_MANY_REQUESTS,
                "UPLOAD_LIMIT_REACHED",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> handleMissingMultipartPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MISSING_MULTIPART_PART",
                "Both note details and a PDF file are required.",
                request,
                Map.of());
    }

    @ExceptionHandler(StorageOperationException.class)
    ResponseEntity<ErrorResponse> handleStorageOperation(
            StorageOperationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "STORAGE_OPERATION_FAILED",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_CREDENTIALS",
                "The email or password is incorrect.",
                request,
                Map.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "AUTH_REFRESH_TOKEN_INVALID",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this action.",
                request,
                Map.of());
    }

    @ExceptionHandler(NoteManagementAccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleNoteManagementAccessDenied(
            NoteManagementAccessDeniedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.FORBIDDEN,
                "NOTE_ADMIN_REQUIRED",
                exception.getMessage(),
                request,
                Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                "The request conflicts with existing data.",
                request,
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error(
                "Unexpected request failure at path {} with trace ID {}",
                request.getRequestURI(),
                request.getRequestId(),
                exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request,
                Map.of());
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, List<String>> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                request.getRequestId(),
                fieldErrors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
