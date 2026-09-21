package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import java.util.List;
import java.util.UUID;

public record StudentTopicTaskResult(
        UUID id,
        String title,
        String descriptionMarkdown,
        TaskType taskType,
        TaskDifficulty difficulty,
        int position,
        boolean required,
        ProgrammingTaskConfig programmingConfig,
        List<TaskTestCase> testCases) {
    public StudentTopicTaskResult {
        testCases = testCases == null ? null : List.copyOf(testCases);
    }
}
