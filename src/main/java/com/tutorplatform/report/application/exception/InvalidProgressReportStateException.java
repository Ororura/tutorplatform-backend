package com.tutorplatform.report.application.exception;

public class InvalidProgressReportStateException extends RuntimeException {
    public InvalidProgressReportStateException(String message) {
        super(message);
    }
}
