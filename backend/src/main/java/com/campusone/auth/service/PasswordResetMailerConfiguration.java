package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class PasswordResetMailerConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PasswordResetMailerConfiguration.class);

    @Bean
    PasswordResetMailer passwordResetMailer(
            PasswordResetProperties properties,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Environment environment,
            ObjectMapper objectMapper) {
        return switch (properties.getMailProvider()) {
            case "resend" -> new ResendPasswordResetMailer(
                    properties,
                    HttpClient.newBuilder()
                            .connectTimeout(properties.getResendTimeout())
                            .build(),
                    objectMapper);
            case "smtp" -> new SmtpPasswordResetMailer(
                    mailSenderProvider,
                    properties,
                    environment);
            case "disabled", "none", "dev" -> new DisabledPasswordResetMailer(
                    properties,
                    environment,
                    "Password reset email provider is disabled.");
            default -> {
                LOGGER.warn(
                        "Unknown password reset mail provider '{}'. Email delivery is disabled.",
                        properties.getMailProvider());
                yield new DisabledPasswordResetMailer(
                        properties,
                        environment,
                        "Unknown password reset email provider: "
                                + properties.getMailProvider());
            }
        };
    }
}
