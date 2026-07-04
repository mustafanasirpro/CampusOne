package com.campusone.moderation.exception;

import org.springframework.security.access.AccessDeniedException;

public class ModeratorAccessDeniedException
        extends AccessDeniedException {

    public ModeratorAccessDeniedException() {
        super("An active moderator assignment is required.");
    }
}
