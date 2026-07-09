package com.campusone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class MailConfigurationLogger implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailConfigurationLogger.class);

    private final PasswordResetProperties passwordResetProperties;
    private final Environment environment;

    public MailConfigurationLogger(
            PasswordResetProperties passwordResetProperties,
            Environment environment) {
        this.passwordResetProperties = passwordResetProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String provider = passwordResetProperties.getMailProvider();
        String host = property("spring.mail.host");
        String port = property("spring.mail.port");
        String username = property("spring.mail.username");
        String password = property("spring.mail.password");
        String from = passwordResetProperties.getMailFrom();

        LOGGER.info(
                "CampusOne email config: provider={}, resendApiKeyPresent={}, resendFrom={}, frontendUrl={}, tokenTtl={}",
                provider,
                hasText(passwordResetProperties.getResendApiKey()),
                display(passwordResetProperties.getResendFrom()),
                display(passwordResetProperties.getFrontendUrl()),
                passwordResetProperties.getTokenTtl());

        if (passwordResetProperties.isProvider("resend")
                && !hasText(passwordResetProperties.getResendApiKey())) {
            LOGGER.warn(
                    "Resend email provider is selected but RESEND_API_KEY is missing.");
        }

        if (!passwordResetProperties.isProvider("smtp")) {
            return;
        }

        LOGGER.info(
                "CampusOne SMTP config: mailEnabled={}, host={}, port={}, usernamePresent={}, from={}",
                passwordResetProperties.isMailEnabled(),
                display(host),
                display(port),
                hasText(username),
                display(from));

        if (passwordResetProperties.isMailEnabled()
                && (!hasText(host)
                        || !hasText(port)
                        || !hasText(username)
                        || !hasText(password)
                        || !hasText(from))) {
            LOGGER.warn("Mail is enabled but SMTP configuration is incomplete.");
        }
    }

    private String property(String name) {
        return environment.getProperty(name, "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String display(String value) {
        return hasText(value) ? value.trim() : "(empty)";
    }
}
