package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_periods")
public class LearningPeriodDatabaseModel {

    @Id
    private UUID id;

    @Column(name = "student_program_id", nullable = false)
    private UUID studentProgramId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "start_cumulative_minutes", nullable = false)
    private int startCumulativeMinutes;

    @Column(name = "target_duration_minutes", nullable = false)
    private int targetDurationMinutes;

    @Column(name = "end_cumulative_minutes")
    private Integer endCumulativeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LearningPeriodStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearningPeriodDatabaseModel() {
    }

    LearningPeriodDatabaseModel(LearningPeriod period) {
        id = period.id();
        studentProgramId = period.studentProgramId();
        sequenceNo = period.sequenceNo();
        startCumulativeMinutes = period.startCumulativeMinutes();
        targetDurationMinutes = period.targetDurationMinutes();
        endCumulativeMinutes = period.endCumulativeMinutes();
        status = period.status();
        startedAt = period.startedAt();
        completedAt = period.completedAt();
        createdAt = period.createdAt();
        updatedAt = period.updatedAt();
    }

    LearningPeriod toDomain() {
        return new LearningPeriod(
            id, studentProgramId, sequenceNo, startCumulativeMinutes, targetDurationMinutes,
            endCumulativeMinutes, status, startedAt, completedAt, createdAt, updatedAt
        );
    }
}
