package com.tutorplatform.session.application;

import java.util.Objects;
import java.util.UUID;

public record LessonSessionTopicInput(UUID topicId, boolean primary) {
    public LessonSessionTopicInput {
        Objects.requireNonNull(topicId);
    }
}
