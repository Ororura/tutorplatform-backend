package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;

public record UpdateTaskCommand(
    String title,
    String descriptionMarkdown,
    TaskDifficulty difficulty,
    TaskStatus status,
    Long version
) {
}
