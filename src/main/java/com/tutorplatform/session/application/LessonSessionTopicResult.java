package com.tutorplatform.session.application;

import java.time.Instant;
import java.util.UUID;

public record LessonSessionTopicResult(
    UUID topicId,
    boolean primary,
    Instant createdAt
) {
}
