package com.tutorplatform.progress.application.exception;

public class ProgressStudentProgramNotFoundException extends RuntimeException {

    public ProgressStudentProgramNotFoundException() {
        super("Student program not found");
    }
}
