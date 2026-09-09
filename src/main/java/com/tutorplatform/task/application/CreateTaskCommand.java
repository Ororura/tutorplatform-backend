package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;

import java.util.List;
import java.util.UUID;

public record CreateTaskCommand(
    UUID subjectId,
    String title,
    String descriptionMarkdown,
    TaskDifficulty difficulty,
    TaskType taskType,
    ProgrammingTaskConfigInput programmingConfig,
    List<TaskTestCaseInput> testCases
) {
    public CreateTaskCommand(UUID subjectId, String title, String descriptionMarkdown, TaskDifficulty difficulty) {
        this(subjectId, title, descriptionMarkdown, difficulty, TaskType.TEXT, null, null);
    }
}
