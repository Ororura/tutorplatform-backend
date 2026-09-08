package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.task.infrastructure.persistence.task.TaskDatabaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "homework_items")
public class HomeworkItemDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, insertable = false, updatable = false)
    private TaskDatabaseModel task;

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean required;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HomeworkItemDatabaseModel() {
    }

    HomeworkItemDatabaseModel(HomeworkItemEntity item) {
        id = Objects.requireNonNull(item.getId());
        taskId = Objects.requireNonNull(item.getTaskId());
        position = item.getPosition();
        required = item.isRequired();
    }

    HomeworkItemEntity toEntity(UUID homeworkId) {
        return new HomeworkItemEntity(id, homeworkId, taskId, position, required, createdAt);
    }

    UUID getId() {
        return id;
    }
}
