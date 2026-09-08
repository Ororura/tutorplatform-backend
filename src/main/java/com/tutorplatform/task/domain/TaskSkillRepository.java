package com.tutorplatform.task.domain;

import java.util.List;
import java.util.UUID;

public interface TaskSkillRepository {
    TaskSkillEntity saveAndFlush(TaskSkillEntity taskSkill);
    List<TaskSkillEntity> findAllByTaskId(UUID taskId);
}
