package com.tutorplatform.session.application.exception;

public class InvalidSessionListParameterException extends RuntimeException {

    private final String field;

    public InvalidSessionListParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
