package com.campusone.moderation.exception;

public class ModerationConflictException extends RuntimeException {

    private final String code;

    public ModerationConflictException(
            String code,
            String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
