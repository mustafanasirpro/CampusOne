package com.campusone.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        Map<String, List<String>> fieldErrors) {

    public static ErrorResponse of(
            int status,
            String error,
            String code,
            String message,
            String path,
            String traceId) {
        return new ErrorResponse(
                Instant.now(),
                status,
                error,
                code,
                message,
                path,
                traceId,
                Map.of());
    }
}
