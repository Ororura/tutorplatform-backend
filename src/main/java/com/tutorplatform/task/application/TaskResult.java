package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;

import java.time.Instant;
import java.util.List;
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
    Instant updatedAt,
    ProgrammingTaskConfig programmingConfig,
    List<TaskTestCase> testCases
) {
}
