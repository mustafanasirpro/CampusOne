package com.campusone.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PasswordResetPropertiesTest {

    @Test
    void defaultsDoNotProvideProductionSenderFallbacks() {
        PasswordResetProperties properties = new PasswordResetProperties();

        assertThat(properties.getResendFrom()).isBlank();
        assertThat(properties.getMailFrom()).isBlank();
    }

    @Test
    void binderMapsConfiguredResendSenderExactly() {
        PasswordResetProperties properties = new PasswordResetProperties();
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "app.password-reset.resend-from",
                "CampusOne <support@mail.campusone.dev>")));

        binder.bind(
                "app.password-reset",
                Bindable.ofInstance(properties));

        assertThat(properties.getResendFrom())
                .isEqualTo("CampusOne <support@mail.campusone.dev>");
    }

    @Test
    void blankSenderCanOverridePreviousValue() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setResendFrom("CampusOne <support@mail.campusone.dev>");
        properties.setMailFrom("CampusOne <support@mail.campusone.dev>");

        properties.setResendFrom("  ");
        properties.setMailFrom("  ");

        assertThat(properties.getResendFrom()).isBlank();
        assertThat(properties.getMailFrom()).isBlank();
    }
}
