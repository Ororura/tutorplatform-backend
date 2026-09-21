package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
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
        List<Item> items) {
    public record Item(
            UUID id,
            UUID taskId,
            int position,
            boolean required,
            boolean passed,
            SubmissionStatus latestSubmissionStatus,
            Task task) {
        public Item withSubmissionState(boolean passed, SubmissionStatus latestSubmissionStatus) {
            return new Item(id, taskId, position, required, passed, latestSubmissionStatus, task);
        }
    }

    public record Task(
            UUID id,
            String title,
            String descriptionMarkdown,
            TaskType taskType,
            TaskDifficulty difficulty,
            CodeExecution codeExecution) {}

    public record CodeExecution(
            ProgrammingLanguage language,
            String starterCode,
            boolean executionEnabled,
            int timeLimitMs,
            int memoryLimitMb) {}
}
