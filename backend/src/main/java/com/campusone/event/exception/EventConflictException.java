package com.campusone.event.exception;

public class EventConflictException extends RuntimeException {

    private final String code;

    public EventConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
