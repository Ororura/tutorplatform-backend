package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TaskQuery {

    Optional<TaskContext> findTask(UUID taskId);

    TaskPage findTeacherTextTasks(
        UUID teacherId,
        UUID subjectId,
        TaskStatus status,
        TaskDifficulty difficulty,
        int page,
        int size,
        String sortField,
        boolean ascending
    );

    List<TaskContext> findTasksByIds(Set<UUID> taskIds);

    record TaskContext(
        UUID id,
        UUID teacherId,
        UUID subjectId,
        String title,
        TaskType type,
        TaskStatus status
    ) {
        public boolean isOwnedBy(UUID expectedTeacherId) {
            return teacherId.equals(expectedTeacherId);
        }
    }
}
