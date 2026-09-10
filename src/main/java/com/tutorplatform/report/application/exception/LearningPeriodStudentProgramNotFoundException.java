package com.tutorplatform.report.application.exception;

import java.util.UUID;

public class LearningPeriodStudentProgramNotFoundException extends RuntimeException {
    public LearningPeriodStudentProgramNotFoundException(UUID studentProgramId) {
        super("StudentProgram not found: " + studentProgramId);
    }
}
