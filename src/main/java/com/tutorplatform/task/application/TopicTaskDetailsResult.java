package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;

import java.util.UUID;

public record TopicTaskDetailsResult(
    UUID taskId,
    String title,
    TaskType taskType,
    TaskDifficulty difficulty,
    TaskStatus status,
    int position,
    boolean required
) {
}
