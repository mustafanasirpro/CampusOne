package com.campusone.common.exception;

public class StorageNotConfiguredException extends RuntimeException {

    public StorageNotConfiguredException() {
        super("File upload is not configured yet.");
    }
}
