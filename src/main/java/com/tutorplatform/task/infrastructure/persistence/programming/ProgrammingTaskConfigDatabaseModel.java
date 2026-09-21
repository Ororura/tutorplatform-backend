package com.tutorplatform.task.infrastructure.persistence.programming;

import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "programming_task_configs")
class ProgrammingTaskConfigDatabaseModel {
    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProgrammingLanguage language;

    @Column(name = "starter_code")
    private String starterCode;

    @Column(name = "execution_enabled", nullable = false)
    private boolean executionEnabled;

    @Column(name = "time_limit_ms", nullable = false)
    private int timeLimitMs;

    @Column(name = "memory_limit_mb", nullable = false)
    private int memoryLimitMb;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProgrammingTaskConfigDatabaseModel() {}

    ProgrammingTaskConfigDatabaseModel(ProgrammingTaskConfig config) {
        taskId = config.taskId();
        language = config.language();
        starterCode = config.starterCode();
        executionEnabled = config.executionEnabled();
        timeLimitMs = config.timeLimitMs();
        memoryLimitMb = config.memoryLimitMb();
        createdAt = config.createdAt();
    }

    ProgrammingTaskConfig toDomain() {
        return new ProgrammingTaskConfig(
                taskId,
                language,
                starterCode,
                executionEnabled,
                timeLimitMs,
                memoryLimitMb,
                createdAt,
                updatedAt);
    }
}
