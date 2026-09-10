package com.tutorplatform.report.application.exception;

public class ProgressReportNotPublishableException extends InvalidProgressReportStateException {
    public ProgressReportNotPublishableException(String message) {
        super(message);
    }
}
