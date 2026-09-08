package com.tutorplatform.task.domain.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    TaskEntity saveAndFlush(TaskEntity task);
    Optional<TaskEntity> findById(UUID taskId);
    Optional<TaskEntity> findOwnedById(UUID taskId, UUID teacherId);
    List<TaskEntity> findAllByTeacherId(UUID teacherId);
    List<TaskEntity> findAllByTeacherId(
            UUID teacherId,
            UUID subjectId,
            TaskStatus status,
            TaskType taskType
    );
}
