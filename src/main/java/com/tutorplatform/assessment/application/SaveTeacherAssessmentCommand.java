package com.tutorplatform.assessment.application;

public record SaveTeacherAssessmentCommand(
    Integer understandingScore,
    Integer independenceScore,
    Integer practiceScore,
    Integer homeworkScore,
    String publicComment
) {
}
