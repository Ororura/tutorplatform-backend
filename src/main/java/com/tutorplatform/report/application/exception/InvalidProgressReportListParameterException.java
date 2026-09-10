package com.tutorplatform.report.application.exception;

public class InvalidProgressReportListParameterException extends RuntimeException {

    private final String field;

    public InvalidProgressReportListParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
