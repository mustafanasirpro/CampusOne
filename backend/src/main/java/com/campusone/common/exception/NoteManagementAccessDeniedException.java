package com.campusone.common.exception;

import org.springframework.security.access.AccessDeniedException;

public class NoteManagementAccessDeniedException
        extends AccessDeniedException {

    public NoteManagementAccessDeniedException() {
        super("Only admins can upload or manage notes.");
    }
}
