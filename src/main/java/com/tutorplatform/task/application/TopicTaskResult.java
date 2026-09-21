package com.tutorplatform.task.application;

import java.time.Instant;
import java.util.UUID;

public record TopicTaskResult(
        UUID topicId, UUID taskId, int position, boolean required, Instant createdAt) {}
