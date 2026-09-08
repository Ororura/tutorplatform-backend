package com.tutorplatform.task.infrastructure.persistence.taskskill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TaskSkillDatabaseRepository extends JpaRepository<TaskSkillDatabaseModel, TaskSkillId> {
    List<TaskSkillDatabaseModel> findAllByIdTaskId(UUID taskId);
}
