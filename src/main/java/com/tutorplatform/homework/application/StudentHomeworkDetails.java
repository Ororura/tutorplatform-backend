package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentHomeworkDetails(
        UUID id,
        UUID studentProgramId,
        String title,
        String description,
        HomeworkStatus status,
        Instant assignedAt,
        Instant dueAt,
        Instant completedAt,
        List<Item> items
) {
    public record Item(
            UUID id,
            UUID taskId,
            int position,
            boolean required,
            Task task
    ) {
    }

    public record Task(
            UUID id,
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty
    ) {
    }
}
