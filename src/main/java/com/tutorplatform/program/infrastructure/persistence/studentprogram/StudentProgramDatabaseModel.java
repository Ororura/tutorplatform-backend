package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.infrastructure.persistence.LearningProgramDatabaseModel;
import com.tutorplatform.student.infrastructure.persistence.StudentDatabaseModel;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_programs")
public class StudentProgramDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, insertable = false, updatable = false)
    private StudentDatabaseModel student;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_program_id", nullable = false, insertable = false, updatable = false)
    private LearningProgramDatabaseModel learningProgram;

    @Column(name = "learning_program_id", nullable = false)
    private UUID learningProgramId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel assignedByTeacher;

    @Column(name = "assigned_by_teacher_id", nullable = false)
    private UUID assignedByTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StudentProgramStatus status;

    @Column(name = "report_interval_minutes", nullable = false)
    private int reportIntervalMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentProgramDatabaseModel() {
    }

    StudentProgramDatabaseModel(StudentProgramEntity studentProgram) {
        id = Objects.requireNonNull(studentProgram.getId());
        updateFrom(studentProgram);
        version = studentProgram.getVersion();
    }

    void updateFrom(StudentProgramEntity studentProgram) {
        studentId = Objects.requireNonNull(studentProgram.getStudentId());
        learningProgramId = Objects.requireNonNull(studentProgram.getLearningProgramId());
        assignedByTeacherId = Objects.requireNonNull(studentProgram.getAssignedByTeacherId());
        status = Objects.requireNonNull(studentProgram.getStatus());
        reportIntervalMinutes = studentProgram.getReportIntervalMinutes();
        startedAt = Objects.requireNonNull(studentProgram.getStartedAt());
        completedAt = studentProgram.getCompletedAt();
    }

    StudentProgramEntity toEntity() {
        return new StudentProgramEntity(
                id, studentId, learningProgramId, assignedByTeacherId, status, reportIntervalMinutes,
                startedAt, completedAt, version, createdAt, updatedAt
        );
    }
}
