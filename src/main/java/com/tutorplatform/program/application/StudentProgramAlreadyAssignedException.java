package com.tutorplatform.program.application;

public class StudentProgramAlreadyAssignedException extends RuntimeException {
    public StudentProgramAlreadyAssignedException(Throwable cause) {
        super(cause);
    }

    public StudentProgramAlreadyAssignedException() {
    }
}
