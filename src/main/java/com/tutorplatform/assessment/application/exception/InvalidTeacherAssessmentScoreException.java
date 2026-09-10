package com.tutorplatform.assessment.application.exception;

public class InvalidTeacherAssessmentScoreException extends RuntimeException {

    public InvalidTeacherAssessmentScoreException(IllegalArgumentException cause) {
        super(cause.getMessage(), cause);
    }
}
