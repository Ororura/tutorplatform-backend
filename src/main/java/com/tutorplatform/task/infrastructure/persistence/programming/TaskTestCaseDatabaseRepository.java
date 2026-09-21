package com.tutorplatform.task.infrastructure.persistence.programming;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskTestCaseDatabaseRepository extends JpaRepository<TaskTestCaseDatabaseModel, UUID> {
    List<TaskTestCaseDatabaseModel> findAllByTaskIdOrderByPosition(UUID taskId);

    void deleteAllByTaskId(UUID taskId);
}
