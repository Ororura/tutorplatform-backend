package com.tutorplatform.submission.application.exception;

public class InvalidSubmissionException extends RuntimeException {

    private final String field;

    public InvalidSubmissionException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
