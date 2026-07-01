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

    @NotEmpty(message = "CORS_ALLOWED_ORIGINS must contain at least one trusted origin")
    private List<String> allowedOrigins = List.of();

    @AssertTrue(message = "CORS origins must be exact HTTP(S) origins without wildcards or paths")
    public boolean isAllowedOriginsValid() {
        return !allowedOrigins.isEmpty()
                && allowedOrigins.stream().allMatch(this::isExactHttpOrigin);
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .toList();
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
