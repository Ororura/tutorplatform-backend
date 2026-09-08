package com.tutorplatform.task.application.exception;

public class InvalidTaskException extends RuntimeException {

    private final String field;

    public InvalidTaskException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
