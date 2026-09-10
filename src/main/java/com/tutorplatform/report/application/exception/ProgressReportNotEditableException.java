package com.tutorplatform.report.application.exception;

public class ProgressReportNotEditableException extends InvalidProgressReportStateException {
    public ProgressReportNotEditableException(String message) {
        super(message);
    }
}
