package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordResetProperties properties;
    private final Environment environment;

    public SmtpPasswordResetMailer(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            PasswordResetProperties properties,
            Environment environment) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void sendResetLink(User user, String rawToken) {
        String link = resetLink(rawToken);
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (properties.isMailEnabled() && mailSender != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getMailFrom());
            message.setTo(user.getEmail());
            message.setSubject("Reset your CampusOne password");
            message.setText("""
                    We received a request to reset your CampusOne password.

                    Open this link within the next %d minutes to choose a new password:
                    %s

                    If you did not request this, you can safely ignore this email.
                    """.formatted(
                    Math.max(1, properties.getTokenTtl().toMinutes()),
                    link));
            mailSender.send(message);
            return;
        }

        if (isLocalProfile()) {
            LOGGER.info(
                    "CampusOne local password reset link for user {}: {}",
                    user.getId(),
                    link);
        } else {
            LOGGER.warn(
                    "Password reset requested for user {}, but mail delivery is disabled.",
                    user.getId());
        }
    }

    private String resetLink(String rawToken) {
        String frontendUrl = properties.getFrontendUrl().replaceAll("/+$", "");
        return frontendUrl
                + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private boolean isLocalProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile ->
                        "local".equalsIgnoreCase(profile)
                                || "dev".equalsIgnoreCase(profile)
                                || "test".equalsIgnoreCase(profile));
    }
}
