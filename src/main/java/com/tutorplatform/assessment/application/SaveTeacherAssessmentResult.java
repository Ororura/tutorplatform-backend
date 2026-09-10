package com.tutorplatform.assessment.application;

public record SaveTeacherAssessmentResult(
    TeacherAssessmentResult assessment,
    boolean created
) {
}
