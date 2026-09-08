package com.tutorplatform.homework.application.exception;

public class InvalidHomeworkException extends RuntimeException {

    private final String field;

    public InvalidHomeworkException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
