package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.task.infrastructure.persistence.task.TaskDatabaseModel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "homework_items")
public class HomeworkItemDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, insertable = false, updatable = false)
    private TaskDatabaseModel task;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean required;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HomeworkItemDatabaseModel() {}

    HomeworkItemDatabaseModel(HomeworkItemEntity item) {
        id = Objects.requireNonNull(item.id());
        taskId = Objects.requireNonNull(item.taskId());
        position = item.position();
        required = item.required();
    }

    HomeworkItemEntity toEntity(UUID homeworkId) {
        return new HomeworkItemEntity(id, homeworkId, taskId, position, required, createdAt);
    }

    UUID getId() {
        return id;
    }
}
