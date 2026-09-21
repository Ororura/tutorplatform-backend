package com.tutorplatform.session.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonSessionRepository {
    LessonSessionEntity saveAndFlush(LessonSessionEntity lessonSession);

    Optional<LessonSessionEntity> findById(UUID lessonSessionId);

    List<LessonSessionEntity> findAll();

    Optional<LessonSessionEntity> findOwnedById(
            UUID lessonSessionId, UUID teacherId, UUID studentId);
}
