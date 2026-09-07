package com.tutorplatform.content.application.exception;

public class LessonMaterialPositionConflictException extends RuntimeException {

    public LessonMaterialPositionConflictException(Throwable cause) {
        super("material position is already used by this topic", cause);
    }
}
