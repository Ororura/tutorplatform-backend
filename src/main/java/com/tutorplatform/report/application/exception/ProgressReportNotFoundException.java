package com.tutorplatform.report.application.exception;

public class ProgressReportNotFoundException extends RuntimeException {
    public ProgressReportNotFoundException() {
        super("ProgressReport not found");
    }
}
