package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
        String link = PasswordResetEmailContent.resetLink(properties, rawToken);
        String body = PasswordResetEmailContent.textBody(properties, link);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (properties.isMailEnabled()) {
            ensureMailReady(mailSender);
            sendMessage(
                    mailSender,
                    user.getEmail(),
                    "CampusOne password reset",
                    body);
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

    @Override
    public void sendTestEmail(String recipientEmail) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!properties.isMailEnabled()) {
            throw new IllegalStateException(
                    "Mail is disabled.");
        }
        ensureMailReady(mailSender);

        sendMessage(
                mailSender,
                recipientEmail,
                "CampusOne test email",
                """
                        CampusOne SMTP is configured correctly.

                        This admin-only diagnostic confirms that the backend can send email.
                        """);
    }

    private void ensureMailReady(JavaMailSender mailSender) {
        if (mailSender == null || !hasRequiredSmtpConfiguration()) {
            throw new IllegalStateException(
                    "Mail is enabled but SMTP configuration is incomplete.");
        }
    }

    private boolean hasRequiredSmtpConfiguration() {
        return hasText(environment.getProperty("spring.mail.host"))
                && hasText(environment.getProperty("spring.mail.port"))
                && hasText(environment.getProperty("spring.mail.username"))
                && hasText(environment.getProperty("spring.mail.password"))
                && hasText(properties.getMailFrom());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void sendMessage(
            JavaMailSender mailSender,
            String recipientEmail,
            String subject,
            String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMailFrom());
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private boolean isLocalProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile ->
                        "local".equalsIgnoreCase(profile)
                                || "dev".equalsIgnoreCase(profile)
                                || "test".equalsIgnoreCase(profile));
    }
}
