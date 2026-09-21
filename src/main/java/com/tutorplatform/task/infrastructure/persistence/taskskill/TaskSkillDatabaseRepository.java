package com.tutorplatform.task.infrastructure.persistence.taskskill;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskSkillDatabaseRepository extends JpaRepository<TaskSkillDatabaseModel, TaskSkillId> {
    List<TaskSkillDatabaseModel> findAllByIdTaskId(UUID taskId);
}
