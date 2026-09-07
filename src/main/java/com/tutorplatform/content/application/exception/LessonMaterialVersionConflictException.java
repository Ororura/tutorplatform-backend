package com.tutorplatform.content.application.exception;

public class LessonMaterialVersionConflictException extends RuntimeException {

    public LessonMaterialVersionConflictException(Throwable cause) {
        super("lesson material was modified by another request", cause);
    }
}
