package com.campusone.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthSessionProperties {

    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(7);

    @NotBlank
    private String refreshTokenCookieName = "campusone_refresh_token";

    private boolean cookieSecure = true;

    private String cookieDomain;

    @AssertTrue(message = "refresh token TTL must be greater than zero")
    public boolean isRefreshTokenTtlPositive() {
        return refreshTokenTtl != null
                && !refreshTokenTtl.isZero()
                && !refreshTokenTtl.isNegative();
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }

    public void setRefreshTokenCookieName(String refreshTokenCookieName) {
        this.refreshTokenCookieName = refreshTokenCookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieDomain() {
        return cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }
}
