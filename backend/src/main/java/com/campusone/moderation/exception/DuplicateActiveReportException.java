package com.campusone.moderation.exception;

public class DuplicateActiveReportException
        extends ModerationConflictException {

    public DuplicateActiveReportException() {
        super(
                "DUPLICATE_ACTIVE_REPORT",
                "An active report for this target already exists.");
    }
}
