package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.program.infrastructure.persistence.studentprogram.StudentProgramDatabaseModel;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "lesson_sessions")
public class LessonSessionDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_program_id", nullable = false, insertable = false, updatable = false)
    private StudentProgramDatabaseModel studentProgram;

    @Column(name = "student_program_id", nullable = false)
    private UUID studentProgramId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel teacher;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 24)
    private AttendanceStatus attendanceStatus;

    @Column private String summary;

    @Column(name = "private_notes")
    private String privateNotes;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonSessionDatabaseModel() {
    }

    LessonSessionDatabaseModel(LessonSessionEntity lessonSession) {
        id = Objects.requireNonNull(lessonSession.id());
        updateFrom(lessonSession);
        version = lessonSession.version();
    }

    void updateFrom(LessonSessionEntity lessonSession) {
        studentProgramId = Objects.requireNonNull(lessonSession.studentProgramId());
        teacherId = Objects.requireNonNull(lessonSession.teacherId());
        startedAt = Objects.requireNonNull(lessonSession.startedAt());
        durationMinutes = lessonSession.durationMinutes();
        attendanceStatus = Objects.requireNonNull(lessonSession.attendanceStatus());
        summary = lessonSession.summary();
        privateNotes = lessonSession.privateNotes();
    }

    LessonSessionEntity toEntity() {
        return new LessonSessionEntity(
                id, studentProgramId, teacherId, startedAt, durationMinutes, attendanceStatus,
                summary, privateNotes, version, createdAt, updatedAt
        );
    }
}
