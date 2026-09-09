package com.tutorplatform.task.application.exception;

public class TaskTypeMismatchException extends RuntimeException {
    public TaskTypeMismatchException() {
        super("Operation is only available for CODE tasks");
    }
}
