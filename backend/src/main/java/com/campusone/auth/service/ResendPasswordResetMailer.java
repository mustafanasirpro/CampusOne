package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendPasswordResetMailer implements PasswordResetMailer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ResendPasswordResetMailer.class);

    private final PasswordResetProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendPasswordResetMailer(
            PasswordResetProperties properties,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendResetLink(User user, String rawToken) {
        String resetLink = PasswordResetEmailContent.resetLink(
                properties,
                rawToken);
        sendEmail(
                user.getEmail(),
                "CampusOne password reset",
                PasswordResetEmailContent.htmlBody(properties, resetLink),
                PasswordResetEmailContent.textBody(properties, resetLink),
                "password reset email");
        LOGGER.info("Password reset email sent for user {}", user.getId());
    }

    @Override
    public void sendTestEmail(String recipientEmail) {
        sendEmail(
                recipientEmail,
                "CampusOne test email",
                """
                        <div style="font-family:Arial,sans-serif;line-height:1.6;color:#0f172a">
                          <h2>CampusOne test email</h2>
                          <p>CampusOne Resend email delivery is configured correctly.</p>
                        </div>
                        """,
                """
                        CampusOne test email

                        CampusOne Resend email delivery is configured correctly.
                        """,
                "test email");
    }

    private void sendEmail(
            String recipientEmail,
            String subject,
            String html,
            String text,
            String description) {
        ensureConfigured();

        HttpRequest request = HttpRequest.newBuilder(properties.getResendApiUrl())
                .timeout(properties.getResendTimeout())
                .header("Authorization", "Bearer " + properties.getResendApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        jsonBody(recipientEmail, subject, html, text)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.error(
                        "Failed to send {} via Resend: status={}, response={}",
                        description,
                        response.statusCode(),
                        safeSummary(response.body()));
                throw new IllegalStateException(
                        "Resend email request failed with status "
                                + response.statusCode());
            }
        } catch (IOException exception) {
            LOGGER.error(
                    "Failed to send {} via Resend: {}: {}",
                    description,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            throw new IllegalStateException(
                    "Resend email request failed.",
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error(
                    "Failed to send {} via Resend: interrupted",
                    description);
            throw new IllegalStateException(
                    "Resend email request was interrupted.",
                    exception);
        }
    }

    private String jsonBody(
            String recipientEmail,
            String subject,
            String html,
            String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", properties.getResendFrom());
        body.put("to", List.of(recipientEmail));
        body.put("subject", subject);
        body.put("html", html);
        body.put("text", text);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Resend email body could not be serialized.",
                    exception);
        }
    }

    private void ensureConfigured() {
        if (!hasText(properties.getResendApiKey())) {
            throw new IllegalStateException(
                    "Resend email provider is selected but RESEND_API_KEY is missing.");
        }
        if (!hasText(properties.getResendFrom())) {
            throw new IllegalStateException(
                    "Resend email provider is selected but RESEND_FROM is missing.");
        }
    }

    private String safeSummary(String responseBody) {
        if (!hasText(responseBody)) {
            return "(empty)";
        }
        String normalized = responseBody.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300
                ? normalized
                : normalized.substring(0, 300) + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
