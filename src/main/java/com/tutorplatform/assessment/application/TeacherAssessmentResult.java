package com.tutorplatform.assessment.application;

import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;

import java.time.Instant;
import java.util.UUID;

public record TeacherAssessmentResult(
    UUID id,
    UUID lessonSessionId,
    Integer understandingScore,
    Integer independenceScore,
    Integer practiceScore,
    Integer homeworkScore,
    String publicComment,
    Instant createdAt,
    Instant updatedAt
) {
    public static TeacherAssessmentResult from(TeacherAssessmentEntity assessment) {
        return new TeacherAssessmentResult(
            assessment.getId(),
            assessment.getLessonSessionId(),
            assessment.getUnderstandingScore(),
            assessment.getIndependenceScore(),
            assessment.getPracticeScore(),
            assessment.getHomeworkScore(),
            assessment.getPublicComment(),
            assessment.getCreatedAt(),
            assessment.getUpdatedAt()
        );
    }
}
