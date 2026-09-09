package com.tutorplatform.task.domain.programming;

import java.util.Optional;
import java.util.UUID;

public interface ProgrammingTaskConfigRepository {
    ProgrammingTaskConfig saveAndFlush(ProgrammingTaskConfig config);
    Optional<ProgrammingTaskConfig> findByTaskId(UUID taskId);
}
