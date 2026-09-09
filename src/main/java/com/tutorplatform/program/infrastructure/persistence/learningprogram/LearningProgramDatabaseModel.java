package com.tutorplatform.program.infrastructure.persistence.learningprogram;

import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.subject.infrastructure.persistence.SubjectDatabaseModel;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "learning_programs")
public class LearningProgramDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel teacher;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, insertable = false, updatable = false)
    private SubjectDatabaseModel subject;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LearningProgramStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearningProgramDatabaseModel() {
    }

    LearningProgramDatabaseModel(LearningProgramEntity learningProgram) {
        id = Objects.requireNonNull(learningProgram.getId());
        updateFrom(learningProgram);
        version = learningProgram.getVersion();
    }

    void updateFrom(LearningProgramEntity learningProgram) {
        teacherId = Objects.requireNonNull(learningProgram.getTeacherId());
        subjectId = Objects.requireNonNull(learningProgram.getSubjectId());
        title = Objects.requireNonNull(learningProgram.getTitle());
        description = learningProgram.getDescription();
        status = Objects.requireNonNull(learningProgram.getStatus());
    }

    LearningProgramEntity toEntity() {
        return new LearningProgramEntity(
            id, teacherId, subjectId, title, description, status, version, createdAt, updatedAt
        );
    }
}
