package com.tutorplatform.session.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface LessonSessionTopicDatabaseRepository
        extends JpaRepository<LessonSessionTopicDatabaseModel, LessonSessionTopicId> {
    List<LessonSessionTopicDatabaseModel> findAllByIdLessonSessionId(UUID lessonSessionId);
}
