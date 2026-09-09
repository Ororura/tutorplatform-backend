package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;

import java.util.UUID;

public record CreateTaskCommand(
    UUID subjectId,
    String title,
    String descriptionMarkdown,
    TaskDifficulty difficulty
) {
}
