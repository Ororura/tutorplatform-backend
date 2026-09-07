package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaLessonSessionTopicRepository implements LessonSessionTopicRepository {

    private final LessonSessionTopicDatabaseRepository databaseRepository;

    JpaLessonSessionTopicRepository(LessonSessionTopicDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LessonSessionTopicEntity saveAndFlush(LessonSessionTopicEntity lessonSessionTopic) {
        return databaseRepository.saveAndFlush(
            new LessonSessionTopicDatabaseModel(lessonSessionTopic)
        ).toEntity();
    }

    @Override
    public List<LessonSessionTopicEntity> findAllByLessonSessionId(UUID lessonSessionId) {
        return databaseRepository.findAllByIdLessonSessionId(lessonSessionId).stream()
            .map(LessonSessionTopicDatabaseModel::toEntity)
            .toList();
    }
}
