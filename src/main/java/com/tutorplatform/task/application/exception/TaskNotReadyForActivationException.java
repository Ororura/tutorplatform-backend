package com.tutorplatform.task.application.exception;

public class TaskNotReadyForActivationException extends RuntimeException {
    public TaskNotReadyForActivationException() {
        super("CODE task requires a valid programming configuration and at least one test case");
    }
}
