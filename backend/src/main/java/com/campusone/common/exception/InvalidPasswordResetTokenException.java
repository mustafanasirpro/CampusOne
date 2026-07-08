package com.campusone.common.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("This reset link is invalid or expired.");
    }
}
