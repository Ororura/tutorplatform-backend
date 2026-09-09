package com.tutorplatform.task.infrastructure.persistence.programming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TaskTestCaseDatabaseRepository extends JpaRepository<TaskTestCaseDatabaseModel, UUID> {
    List<TaskTestCaseDatabaseModel> findAllByTaskIdOrderByPosition(UUID taskId);
    void deleteAllByTaskId(UUID taskId);
}
