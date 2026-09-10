package com.tutorplatform.assessment.infrastructure.persistence;

import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;
import com.tutorplatform.assessment.domain.TeacherAssessmentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTeacherAssessmentRepository implements TeacherAssessmentRepository {

    private final TeacherAssessmentDatabaseRepository databaseRepository;

    JpaTeacherAssessmentRepository(TeacherAssessmentDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public TeacherAssessmentEntity saveAndFlush(TeacherAssessmentEntity assessment) {
        TeacherAssessmentDatabaseModel databaseModel = databaseRepository.findById(assessment.getId())
            .map(existing -> {
                existing.updateFrom(assessment);
                return existing;
            })
            .orElseGet(() -> new TeacherAssessmentDatabaseModel(assessment));
        return databaseRepository.saveAndFlush(databaseModel).toEntity();
    }

    @Override
    public Optional<TeacherAssessmentEntity> findById(UUID assessmentId) {
        return databaseRepository.findById(assessmentId).map(TeacherAssessmentDatabaseModel::toEntity);
    }

    @Override
    public Optional<TeacherAssessmentEntity> findByLessonSessionId(UUID lessonSessionId) {
        return databaseRepository.findByLessonSessionId(lessonSessionId)
            .map(TeacherAssessmentDatabaseModel::toEntity);
    }
}
