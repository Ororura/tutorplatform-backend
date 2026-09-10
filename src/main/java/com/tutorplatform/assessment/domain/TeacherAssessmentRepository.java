package com.tutorplatform.assessment.domain;

import java.util.Optional;
import java.util.UUID;

public interface TeacherAssessmentRepository {
    TeacherAssessmentEntity saveAndFlush(TeacherAssessmentEntity assessment);

    Optional<TeacherAssessmentEntity> findById(UUID assessmentId);

    Optional<TeacherAssessmentEntity> findByLessonSessionId(UUID lessonSessionId);
}
