package com.tutorplatform.session.domain;

import java.util.List;
import java.util.UUID;

public interface LessonSessionTopicRepository {
    LessonSessionTopicEntity saveAndFlush(LessonSessionTopicEntity lessonSessionTopic);
    List<LessonSessionTopicEntity> findAllByLessonSessionId(UUID lessonSessionId);
}
