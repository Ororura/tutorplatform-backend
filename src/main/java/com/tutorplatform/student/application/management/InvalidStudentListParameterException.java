package com.tutorplatform.student.application.management;

public class InvalidStudentListParameterException extends RuntimeException {

    private final String field;

    public InvalidStudentListParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
