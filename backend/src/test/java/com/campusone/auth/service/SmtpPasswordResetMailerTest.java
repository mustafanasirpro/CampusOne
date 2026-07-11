package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpPasswordResetMailerTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment environment;

    private PasswordResetProperties properties;
    private SmtpPasswordResetMailer mailer;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setMailEnabled(true);
        properties.setMailFrom("CampusOne <support@mail.campusone.dev>");
        properties.setFrontendUrl("https://campusone.dev");
        properties.setTokenTtl(Duration.ofMinutes(30));
        mailer = new SmtpPasswordResetMailer(
                mailSenderProvider,
                properties,
                environment);
    }

    @Test
    void sendResetLinkSendsProductionResetLinkWithoutExposingTokenElsewhere() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        stubCompleteSmtpConfig();
        User user = new User("student@example.com", "$2a$12$password");
        ReflectionTestUtils.setField(
                user,
                "id",
                UUID.fromString("10000000-0000-4000-8000-000000000001"));

        mailer.sendResetLink(user, "raw-reset-token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom())
                .isEqualTo("CampusOne <support@mail.campusone.dev>");
        assertThat(message.getTo()).containsExactly("student@example.com");
        assertThat(message.getSubject()).isEqualTo("CampusOne password reset");
        assertThat(message.getText())
                .contains("https://campusone.dev/reset-password?token=raw-reset-token")
                .contains("This link expires in 30 minutes")
                .contains("If you did not request this");
    }

    @Test
    void sendTestEmailUsesSameSmtpSender() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        stubCompleteSmtpConfig();

        mailer.sendTestEmail("admin@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("CampusOne test email");
        assertThat(message.getText()).contains("SMTP is configured correctly");
    }

    @Test
    void sendTestEmailWhenMailDisabledFailsSafely() {
        properties.setMailEnabled(false);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        assertThatThrownBy(() -> mailer.sendTestEmail("admin@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mail is disabled");
    }

    @Test
    void sendTestEmailWhenSmtpConfigIncompleteFailsFast() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        assertThatThrownBy(() -> mailer.sendTestEmail("admin@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP configuration is incomplete");
    }

    private void stubCompleteSmtpConfig() {
        when(environment.getProperty("spring.mail.host"))
                .thenReturn("smtp.gmail.com");
        when(environment.getProperty("spring.mail.port"))
                .thenReturn("587");
        when(environment.getProperty("spring.mail.username"))
                .thenReturn("sender@example.com");
        when(environment.getProperty("spring.mail.password"))
                .thenReturn("abcdefghijklmnop");
    }
}
