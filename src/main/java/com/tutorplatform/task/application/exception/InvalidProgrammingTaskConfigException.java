package com.tutorplatform.task.application.exception;

public class InvalidProgrammingTaskConfigException extends RuntimeException {
    private final String field;

    public InvalidProgrammingTaskConfigException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
