package com.campusone.auth.service;

import com.campusone.config.PasswordResetProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class PasswordResetEmailContent {

    private PasswordResetEmailContent() {
    }

    static String resetLink(
            PasswordResetProperties properties,
            String rawToken) {
        String frontendUrl = properties.getFrontendUrl().replaceAll("/+$", "");
        return frontendUrl
                + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    static String textBody(
            PasswordResetProperties properties,
            String resetLink) {
        return """
                Hi,

                We received a request to reset your CampusOne password.

                Reset your password here:
                %s

                This link expires in %d minutes and can be used only once.

                If you did not request this, you can safely ignore this email.
                """.formatted(
                resetLink,
                Math.max(1, properties.getTokenTtl().toMinutes()));
    }

    static String htmlBody(
            PasswordResetProperties properties,
            String resetLink) {
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6;color:#0f172a">
                  <h2 style="margin:0 0 12px">CampusOne password reset</h2>
                  <p>Hi,</p>
                  <p>We received a request to reset your CampusOne password.</p>
                  <p>
                    <a href="%s" style="display:inline-block;background:#2563eb;color:#ffffff;padding:10px 16px;border-radius:8px;text-decoration:none">
                      Reset your password
                    </a>
                  </p>
                  <p>If the button does not work, copy and paste this link into your browser:</p>
                  <p style="word-break:break-all">%s</p>
                  <p>This link expires in %d minutes and can be used only once.</p>
                  <p>If you did not request this, you can ignore this email.</p>
                </div>
                """.formatted(
                resetLink,
                resetLink,
                Math.max(1, properties.getTokenTtl().toMinutes()));
    }
}
