package com.campusone.internship.exception;

public class InternshipConflictException extends RuntimeException {

    private final String code;

    public InternshipConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
