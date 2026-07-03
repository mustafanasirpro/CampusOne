package com.campusone.search.exception;

public class SearchValidationException extends RuntimeException {

    private final String field;

    public SearchValidationException(
            String field,
            String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
