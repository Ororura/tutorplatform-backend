package com.tutorplatform.session.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LessonSessionTopicRepository {
    LessonSessionTopicEntity saveAndFlush(LessonSessionTopicEntity lessonSessionTopic);

    List<LessonSessionTopicEntity> saveAllAndFlush(List<LessonSessionTopicEntity> lessonSessionTopics);

    List<LessonSessionTopicEntity> findAllByLessonSessionId(UUID lessonSessionId);

    List<LessonSessionTopicEntity> findAllByLessonSessionIds(Set<UUID> lessonSessionIds);

    void deleteAllByLessonSessionId(UUID lessonSessionId);
}
