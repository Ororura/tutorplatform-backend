package com.tutorplatform.task.application.exception;

public class InvalidTaskListParameterException extends RuntimeException {

    private final String field;

    public InvalidTaskListParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
