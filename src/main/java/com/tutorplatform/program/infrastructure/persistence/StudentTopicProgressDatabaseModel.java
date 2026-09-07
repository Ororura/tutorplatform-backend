package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.StudentTopicProgressStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_topic_progress")
public class StudentTopicProgressDatabaseModel {

    @EmbeddedId
    private StudentTopicProgressId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_program_id", nullable = false, insertable = false, updatable = false)
    private StudentProgramDatabaseModel studentProgram;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, insertable = false, updatable = false)
    private TopicDatabaseModel topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StudentTopicProgressStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentTopicProgressDatabaseModel() {
    }

    StudentTopicProgressDatabaseModel(StudentTopicProgressEntity progress) {
        id = new StudentTopicProgressId(progress.getStudentProgramId(), progress.getTopicId());
        updateFrom(progress);
    }

    void updateFrom(StudentTopicProgressEntity progress) {
        status = Objects.requireNonNull(progress.getStatus());
        startedAt = progress.getStartedAt();
        completedAt = progress.getCompletedAt();
    }

    StudentTopicProgressEntity toEntity() {
        return new StudentTopicProgressEntity(
                id.getStudentProgramId(), id.getTopicId(), status, startedAt, completedAt, updatedAt
        );
    }
}
