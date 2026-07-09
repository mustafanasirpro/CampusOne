package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.campusone.config.PasswordResetProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordResetMailerConfigurationTest {

    private final PasswordResetMailerConfiguration configuration =
            new PasswordResetMailerConfiguration();
    private final ObjectProvider<JavaMailSender> mailSenderProvider =
            mock(ObjectProvider.class);
    private final Environment environment = mock(Environment.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resendProviderUsesResendMailerInsteadOfSmtp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setMailProvider("resend");

        PasswordResetMailer mailer = configuration.passwordResetMailer(
                properties,
                mailSenderProvider,
                environment,
                objectMapper);

        assertThat(mailer).isInstanceOf(ResendPasswordResetMailer.class);
    }

    @Test
    void smtpProviderUsesSmtpMailerOnlyWhenSelected() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setMailProvider("smtp");

        PasswordResetMailer mailer = configuration.passwordResetMailer(
                properties,
                mailSenderProvider,
                environment,
                objectMapper);

        assertThat(mailer).isInstanceOf(SmtpPasswordResetMailer.class);
    }

    @Test
    void missingProviderDefaultsToDisabledMailer() {
        PasswordResetProperties properties = new PasswordResetProperties();

        PasswordResetMailer mailer = configuration.passwordResetMailer(
                properties,
                mailSenderProvider,
                environment,
                objectMapper);

        assertThat(mailer).isInstanceOf(DisabledPasswordResetMailer.class);
    }
}
