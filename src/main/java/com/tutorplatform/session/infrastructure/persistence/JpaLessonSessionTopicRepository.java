package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLessonSessionTopicRepository implements LessonSessionTopicRepository {

    private final LessonSessionTopicDatabaseRepository databaseRepository;

    JpaLessonSessionTopicRepository(LessonSessionTopicDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LessonSessionTopicEntity saveAndFlush(LessonSessionTopicEntity lessonSessionTopic) {
        return databaseRepository
                .saveAndFlush(new LessonSessionTopicDatabaseModel(lessonSessionTopic))
                .toEntity();
    }

    @Override
    public List<LessonSessionTopicEntity> saveAllAndFlush(
            List<LessonSessionTopicEntity> lessonSessionTopics) {
        return databaseRepository
                .saveAllAndFlush(
                        lessonSessionTopics.stream()
                                .map(LessonSessionTopicDatabaseModel::new)
                                .toList())
                .stream()
                .map(LessonSessionTopicDatabaseModel::toEntity)
                .toList();
    }

    @Override
    public List<LessonSessionTopicEntity> findAllByLessonSessionId(UUID lessonSessionId) {
        return databaseRepository.findAllByIdLessonSessionId(lessonSessionId).stream()
                .map(LessonSessionTopicDatabaseModel::toEntity)
                .toList();
    }

    @Override
    public List<LessonSessionTopicEntity> findAllByLessonSessionIds(Set<UUID> lessonSessionIds) {
        if (lessonSessionIds.isEmpty()) {
            return List.of();
        }
        return databaseRepository.findAllByIdLessonSessionIdIn(lessonSessionIds).stream()
                .map(LessonSessionTopicDatabaseModel::toEntity)
                .toList();
    }

    @Override
    public void deleteAllByLessonSessionId(UUID lessonSessionId) {
        databaseRepository.deleteAllByLessonSessionId(lessonSessionId);
    }
}
