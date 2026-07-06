package com.campusone.common.exception;

public class FileUploadTooLargeException extends RuntimeException {

    public FileUploadTooLargeException(int maximumSizeMb) {
        super("File size must be " + maximumSizeMb + " MB or less.");
    }
}
