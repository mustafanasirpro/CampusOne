package com.campusone.common.exception;

public class StorageNotConfiguredException extends RuntimeException {

    public StorageNotConfiguredException() {
        super("Storage is temporarily unavailable.");
    }
}
