package com.tutorplatform.task.application.exception;

public class InvalidTaskTestCaseException extends RuntimeException {
    private final String field;

    public InvalidTaskTestCaseException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
