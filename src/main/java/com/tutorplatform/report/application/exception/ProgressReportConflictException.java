package com.tutorplatform.report.application.exception;

public class ProgressReportConflictException extends RuntimeException {
    public ProgressReportConflictException() {
        super("A ProgressReport already exists for this LearningPeriod");
    }

    public ProgressReportConflictException(Throwable cause) {
        super("A ProgressReport already exists for this LearningPeriod", cause);
    }
}
