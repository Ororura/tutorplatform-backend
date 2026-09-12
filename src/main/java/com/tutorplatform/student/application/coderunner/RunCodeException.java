package com.tutorplatform.student.application.coderunner;

public class RunCodeException extends RuntimeException {

    public enum Reason {
        TASK_NOT_FOUND,
        TASK_NOT_EXECUTABLE,
        TASK_EXECUTION_DISABLED,
        EXECUTION_CONTEXT_INVALID
    }

    private final Reason reason;

    public RunCodeException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
