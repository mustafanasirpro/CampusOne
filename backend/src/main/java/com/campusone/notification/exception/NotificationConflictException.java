package com.campusone.notification.exception;

public class NotificationConflictException extends RuntimeException {

    private final String code;

    public NotificationConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
