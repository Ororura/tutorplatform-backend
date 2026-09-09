package com.tutorplatform.task.infrastructure.persistence.task;

import com.tutorplatform.subject.infrastructure.persistence.SubjectDatabaseModel;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class TaskDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel teacher;

    @Column(name = "teacher_id", nullable = false, updatable = false)
    private UUID teacherId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, insertable = false, updatable = false)
    private SubjectDatabaseModel subject;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(name = "description_markdown", nullable = false)
    private String descriptionMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 32)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TaskStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskDatabaseModel() {
    }

    TaskDatabaseModel(TaskEntity task) {
        id = Objects.requireNonNull(task.getId());
        teacherId = Objects.requireNonNull(task.getTeacherId());
        subjectId = Objects.requireNonNull(task.getSubjectId());
        updateFrom(task);
        version = task.getVersion();
    }

    void updateFrom(TaskEntity task) {
        if (!Objects.requireNonNull(task.getTeacherId()).equals(teacherId)
            || !Objects.requireNonNull(task.getSubjectId()).equals(subjectId)) {
            throw new IllegalArgumentException("Task ownership and subject cannot be changed");
        }
        title = Objects.requireNonNull(task.getTitle());
        descriptionMarkdown = Objects.requireNonNull(task.getDescriptionMarkdown());
        taskType = Objects.requireNonNull(task.getTaskType());
        difficulty = Objects.requireNonNull(task.getDifficulty());
        status = Objects.requireNonNull(task.getStatus());
    }

    TaskEntity toEntity() {
        return new TaskEntity(
            id, teacherId, subjectId, title, descriptionMarkdown, taskType, difficulty, status,
            version, createdAt, updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getTitle() {
        return title;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }
}
