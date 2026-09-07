package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.session.domain.LessonSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLessonSessionRepository implements LessonSessionRepository {

    private final LessonSessionDatabaseRepository databaseRepository;

    JpaLessonSessionRepository(LessonSessionDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LessonSessionEntity saveAndFlush(LessonSessionEntity lessonSession) {
        return databaseRepository.saveAndFlush(new LessonSessionDatabaseModel(lessonSession)).toEntity();
    }

    @Override
    public Optional<LessonSessionEntity> findById(UUID lessonSessionId) {
        return databaseRepository.findById(lessonSessionId).map(LessonSessionDatabaseModel::toEntity);
    }

    @Override
    public List<LessonSessionEntity> findAll() {
        return databaseRepository.findAll().stream().map(LessonSessionDatabaseModel::toEntity).toList();
    }

    @Override
    public Optional<LessonSessionEntity> findOwnedById(
        UUID lessonSessionId,
        UUID teacherId,
        UUID studentId
    ) {
        return databaseRepository.findOwnedById(lessonSessionId, teacherId, studentId)
            .map(LessonSessionDatabaseModel::toEntity);
    }

}
