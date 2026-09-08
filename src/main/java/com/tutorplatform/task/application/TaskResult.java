package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;

import java.time.Instant;
import java.util.UUID;

public record TaskResult(
        UUID id,
        UUID subjectId,
        String title,
        String descriptionMarkdown,
        TaskType taskType,
        TaskDifficulty difficulty,
        TaskStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
