package com.campusone.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private Duration tokenTtl = Duration.ofMinutes(30);
    private String frontendUrl = "http://localhost:5173";
    private boolean mailEnabled;
    private String mailFrom = "CampusOne <no-reply@campusone.dev>";

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl == null ? Duration.ofMinutes(30) : tokenTtl;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            this.frontendUrl = frontendUrl.trim();
        }
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public void setMailEnabled(boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        if (mailFrom != null && !mailFrom.isBlank()) {
            this.mailFrom = mailFrom.trim();
        }
    }
}
