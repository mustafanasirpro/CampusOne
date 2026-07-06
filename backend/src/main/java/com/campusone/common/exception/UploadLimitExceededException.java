package com.campusone.common.exception;

public class UploadLimitExceededException extends RuntimeException {

    public UploadLimitExceededException(String message) {
        super(message);
    }
}
