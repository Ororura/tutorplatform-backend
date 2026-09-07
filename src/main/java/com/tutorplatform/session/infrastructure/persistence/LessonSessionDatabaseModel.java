package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.program.infrastructure.persistence.studentprogram.StudentProgramDatabaseModel;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.session.domain.LessonSessionEntity;
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
        id = Objects.requireNonNull(lessonSession.getId());
        updateFrom(lessonSession);
        version = lessonSession.getVersion();
    }

    void updateFrom(LessonSessionEntity lessonSession) {
        studentProgramId = Objects.requireNonNull(lessonSession.getStudentProgramId());
        teacherId = Objects.requireNonNull(lessonSession.getTeacherId());
        startedAt = Objects.requireNonNull(lessonSession.getStartedAt());
        durationMinutes = lessonSession.getDurationMinutes();
        attendanceStatus = Objects.requireNonNull(lessonSession.getAttendanceStatus());
        summary = lessonSession.getSummary();
        privateNotes = lessonSession.getPrivateNotes();
    }

    LessonSessionEntity toEntity() {
        return new LessonSessionEntity(
                id, studentProgramId, teacherId, startedAt, durationMinutes, attendanceStatus,
                summary, privateNotes, version, createdAt, updatedAt
        );
    }
}
