package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.TaskTestCase;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TaskQuery {

    Optional<TaskContext> findTask(UUID taskId);

    Optional<CodeTaskConfiguration> findCodeTaskConfiguration(UUID taskId);

    TaskPage findTeacherTasks(
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

    record CodeTaskConfiguration(
        UUID id,
        TaskType type,
        TaskStatus status,
        ProgrammingTaskConfig programmingConfig,
        List<TaskTestCase> testCases
    ) {
        public CodeTaskConfiguration {
            testCases = testCases == null ? List.of() : List.copyOf(testCases);
        }
    }
}
