package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

class DisabledPasswordResetMailer implements PasswordResetMailer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DisabledPasswordResetMailer.class);

    private final PasswordResetProperties properties;
    private final Environment environment;
    private final String reason;

    DisabledPasswordResetMailer(
            PasswordResetProperties properties,
            Environment environment,
            String reason) {
        this.properties = properties;
        this.environment = environment;
        this.reason = reason;
    }

    @Override
    public void sendResetLink(User user, String rawToken) {
        String link = PasswordResetEmailContent.resetLink(properties, rawToken);
        if (isLocalProfile()) {
            LOGGER.info(
                    "CampusOne local password reset link for user {}: {}",
                    user.getId(),
                    link);
            return;
        }
        throw new IllegalStateException(reason);
    }

    @Override
    public void sendTestEmail(String recipientEmail) {
        throw new IllegalStateException(reason);
    }

    private boolean isLocalProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile ->
                        "local".equalsIgnoreCase(profile)
                                || "dev".equalsIgnoreCase(profile)
                                || "test".equalsIgnoreCase(profile));
    }
}
