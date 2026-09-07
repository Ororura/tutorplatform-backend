package com.tutorplatform.session.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class LessonSessionTopicId implements Serializable {

    @Column(name = "lesson_session_id", nullable = false)
    private UUID lessonSessionId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    protected LessonSessionTopicId() {
    }

    public LessonSessionTopicId(UUID lessonSessionId, UUID topicId) {
        this.lessonSessionId = Objects.requireNonNull(lessonSessionId);
        this.topicId = Objects.requireNonNull(topicId);
    }

    public UUID getLessonSessionId() { return lessonSessionId; }
    public UUID getTopicId() { return topicId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LessonSessionTopicId that)) return false;
        return lessonSessionId.equals(that.lessonSessionId) && topicId.equals(that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessonSessionId, topicId);
    }
}
