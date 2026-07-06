package com.campusone.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank
    private String cookieSameSite = "None";

    @Positive
    private int maxLoginAttempts = 5;

    @NotNull
    private Duration accountLockDuration = Duration.ofMinutes(15);

    @AssertTrue(message = "refresh token TTL must be greater than zero")
    public boolean isRefreshTokenTtlPositive() {
        return refreshTokenTtl != null
                && !refreshTokenTtl.isZero()
                && !refreshTokenTtl.isNegative();
    }

    @AssertTrue(message = "cookie SameSite must be Strict, Lax, or None")
    public boolean isCookieSameSiteValid() {
        return "Strict".equalsIgnoreCase(cookieSameSite)
                || "Lax".equalsIgnoreCase(cookieSameSite)
                || "None".equalsIgnoreCase(cookieSameSite);
    }

    @AssertTrue(message = "SameSite=None refresh cookies must be secure")
    public boolean isSameSiteNoneSecure() {
        return !"None".equalsIgnoreCase(cookieSameSite) || cookieSecure;
    }

    @AssertTrue(message = "account lock duration must be greater than zero")
    public boolean isAccountLockDurationPositive() {
        return accountLockDuration != null
                && !accountLockDuration.isZero()
                && !accountLockDuration.isNegative();
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

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite == null
                ? ""
                : cookieSameSite.trim();
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public Duration getAccountLockDuration() {
        return accountLockDuration;
    }

    public void setAccountLockDuration(Duration accountLockDuration) {
        this.accountLockDuration = accountLockDuration;
    }
}
