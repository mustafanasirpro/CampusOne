package com.campusone.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://campus-one-ruby.vercel.app",
            "https://campusone.dev",
            "https://www.campusone.dev");

    @NotEmpty(message = "APP_CORS_ALLOWED_ORIGINS must contain at least one trusted origin")
    private List<String> allowedOrigins = DEFAULT_ALLOWED_ORIGINS;

    @AssertTrue(message = "CORS origins must be exact HTTP(S) origins without wildcards or paths")
    public boolean isAllowedOriginsValid() {
        return !allowedOrigins.isEmpty()
                && allowedOrigins.stream().allMatch(this::isExactHttpOrigin);
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        List<String> normalizedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(this::normalizeOrigin)
                        .filter(value -> !value.isEmpty())
                        .toList();
        this.allowedOrigins = java.util.stream.Stream
                .concat(DEFAULT_ALLOWED_ORIGINS.stream(), normalizedOrigins.stream())
                .distinct()
                .toList();
    }

    private String normalizeOrigin(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > "https://".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isExactHttpOrigin(String value) {
        try {
            URI origin = URI.create(value);
            boolean supportedScheme = "https".equalsIgnoreCase(origin.getScheme())
                    || "http".equalsIgnoreCase(origin.getScheme());
            String path = origin.getPath();
            return supportedScheme
                    && origin.getHost() != null
                    && origin.getUserInfo() == null
                    && (path == null || path.isEmpty())
                    && origin.getQuery() == null
                    && origin.getFragment() == null
                    && !value.contains("*");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
