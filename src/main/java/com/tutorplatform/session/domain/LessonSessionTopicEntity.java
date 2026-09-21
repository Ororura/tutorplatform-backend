package com.tutorplatform.session.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LessonSessionTopicEntity(
        UUID lessonSessionId, UUID topicId, boolean primary, Instant createdAt) {

    public LessonSessionTopicEntity(UUID lessonSessionId, UUID topicId, boolean primary) {
        this(lessonSessionId, topicId, primary, null);
    }

    public LessonSessionTopicEntity(
            UUID lessonSessionId, UUID topicId, boolean primary, Instant createdAt) {
        this.lessonSessionId = Objects.requireNonNull(lessonSessionId);
        this.topicId = Objects.requireNonNull(topicId);
        this.primary = primary;
        this.createdAt = createdAt;
    }
}
