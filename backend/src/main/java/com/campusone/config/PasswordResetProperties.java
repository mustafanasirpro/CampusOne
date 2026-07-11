package com.campusone.config;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private Duration tokenTtl = Duration.ofMinutes(30);
    private String frontendUrl = "http://localhost:5173";
    private String mailProvider = "disabled";
    private boolean mailEnabled;
    private String mailFrom = "";
    private String resendApiKey = "";
    private String resendFrom = "";
    private URI resendApiUrl = URI.create("https://api.resend.com/emails");
    private Duration resendTimeout = Duration.ofSeconds(10);

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

    public String getMailProvider() {
        return mailProvider;
    }

    public void setMailProvider(String mailProvider) {
        if (mailProvider != null && !mailProvider.isBlank()) {
            this.mailProvider = mailProvider.trim().toLowerCase(Locale.ROOT);
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
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    public String getResendApiKey() {
        return resendApiKey;
    }

    public void setResendApiKey(String resendApiKey) {
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
    }

    public String getResendFrom() {
        return resendFrom;
    }

    public void setResendFrom(String resendFrom) {
        this.resendFrom = resendFrom == null ? "" : resendFrom.trim();
    }

    public URI getResendApiUrl() {
        return resendApiUrl;
    }

    public void setResendApiUrl(URI resendApiUrl) {
        if (resendApiUrl != null) {
            this.resendApiUrl = resendApiUrl;
        }
    }

    public Duration getResendTimeout() {
        return resendTimeout;
    }

    public void setResendTimeout(Duration resendTimeout) {
        this.resendTimeout = resendTimeout == null
                ? Duration.ofSeconds(10)
                : resendTimeout;
    }

    public boolean isProvider(String provider) {
        return mailProvider.equalsIgnoreCase(provider);
    }
}
