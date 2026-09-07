package com.tutorplatform.session.application.exception;

public class InvalidSessionListParameterException extends RuntimeException {
    public InvalidSessionListParameterException(String field, String message) {
        super(field + " " + message);
    }
}
