package com.tutorplatform.report.application.exception;

public class ProgressReportVersionConflictException extends RuntimeException {
    public ProgressReportVersionConflictException(Throwable cause) {
        super("ProgressReport was changed concurrently", cause);
    }
}
