package com.tutorplatform.task.infrastructure.persistence.taskskill;

import com.tutorplatform.task.domain.taskskill.TaskSkillEntity;
import com.tutorplatform.task.infrastructure.persistence.skill.SkillDatabaseModel;
import com.tutorplatform.task.infrastructure.persistence.task.TaskDatabaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "task_skills")
public class TaskSkillDatabaseModel {

    @EmbeddedId
    private TaskSkillId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, insertable = false, updatable = false)
    private TaskDatabaseModel task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false, insertable = false, updatable = false)
    private SkillDatabaseModel skill;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskSkillDatabaseModel() {
    }

    TaskSkillDatabaseModel(TaskSkillEntity taskSkill) {
        id = new TaskSkillId(taskSkill.getTaskId(), taskSkill.getSkillId());
    }

    TaskSkillEntity toEntity() {
        return new TaskSkillEntity(id.getTaskId(), id.getSkillId(), createdAt);
    }
}
