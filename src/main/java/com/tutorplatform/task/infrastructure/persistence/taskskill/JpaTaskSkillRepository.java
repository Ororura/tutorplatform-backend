package com.tutorplatform.task.infrastructure.persistence.taskskill;

import com.tutorplatform.task.domain.taskskill.TaskSkillEntity;
import com.tutorplatform.task.domain.taskskill.TaskSkillRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaTaskSkillRepository implements TaskSkillRepository {

    private final TaskSkillDatabaseRepository databaseRepository;

    JpaTaskSkillRepository(TaskSkillDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public TaskSkillEntity saveAndFlush(TaskSkillEntity taskSkill) {
        return databaseRepository.saveAndFlush(new TaskSkillDatabaseModel(taskSkill)).toEntity();
    }

    @Override
    public List<TaskSkillEntity> findAllByTaskId(UUID taskId) {
        return databaseRepository.findAllByIdTaskId(taskId).stream()
            .map(TaskSkillDatabaseModel::toEntity)
            .toList();
    }
}
