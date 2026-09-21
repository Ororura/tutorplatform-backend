package com.tutorplatform.assessment.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeacherAssessmentDatabaseRepository
        extends JpaRepository<TeacherAssessmentDatabaseModel, UUID> {

    Optional<TeacherAssessmentDatabaseModel> findByLessonSessionId(UUID lessonSessionId);
}
