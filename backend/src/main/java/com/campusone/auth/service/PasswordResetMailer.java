package com.campusone.auth.service;

import com.campusone.user.entity.User;

public interface PasswordResetMailer {

    void sendResetLink(User user, String rawToken);

    void sendTestEmail(String recipientEmail);
}
