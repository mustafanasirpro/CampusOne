package com.campusone.common.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("The refresh token is missing, expired, revoked, or invalid.");
    }
}
