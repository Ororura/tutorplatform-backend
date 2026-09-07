package com.tutorplatform.session.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LessonSessionTopicEntity {

    private final UUID lessonSessionId;
    private final UUID topicId;
    private final boolean primary;
    private final Instant createdAt;

    public LessonSessionTopicEntity(UUID lessonSessionId, UUID topicId, boolean primary) {
        this(lessonSessionId, topicId, primary, null);
    }

    public LessonSessionTopicEntity(UUID lessonSessionId, UUID topicId, boolean primary, Instant createdAt) {
        this.lessonSessionId = Objects.requireNonNull(lessonSessionId);
        this.topicId = Objects.requireNonNull(topicId);
        this.primary = primary;
        this.createdAt = createdAt;
    }

    public UUID getLessonSessionId() { return lessonSessionId; }
    public UUID getTopicId() { return topicId; }
    public boolean isPrimary() { return primary; }
    public Instant getCreatedAt() { return createdAt; }
}
