package com.tutorplatform.dashboard.application;

public class InvalidTeacherDashboardAttentionParameterException extends RuntimeException {

    private final String field;

    public InvalidTeacherDashboardAttentionParameterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
