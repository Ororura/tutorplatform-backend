package com.tutorplatform.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TeacherAssessmentDatabaseRepository
    extends JpaRepository<TeacherAssessmentDatabaseModel, UUID> {

    Optional<TeacherAssessmentDatabaseModel> findByLessonSessionId(UUID lessonSessionId);
}
