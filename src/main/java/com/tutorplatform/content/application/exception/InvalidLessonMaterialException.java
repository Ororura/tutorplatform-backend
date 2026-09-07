package com.tutorplatform.content.application.exception;

public class InvalidLessonMaterialException extends RuntimeException {

    private final String field;

    public InvalidLessonMaterialException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
