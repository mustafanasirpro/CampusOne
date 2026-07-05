package com.campusone.common.exception;

public class FileUploadTooLargeException extends RuntimeException {

    public FileUploadTooLargeException(int maximumSizeMb) {
        super("The PDF must not exceed " + maximumSizeMb + " MB.");
    }
}
