package com.campusone.moderation.exception;

public class InvalidReportStatusTransitionException
        extends ModerationConflictException {

    public InvalidReportStatusTransitionException(String message) {
        super("INVALID_REPORT_STATUS_TRANSITION", message);
    }
}
