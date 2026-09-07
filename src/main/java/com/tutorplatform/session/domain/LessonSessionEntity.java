package com.tutorplatform.session.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LessonSessionEntity {

    private final UUID id;
    private final UUID studentProgramId;
    private final UUID teacherId;
    private final Instant startedAt;
    private final int durationMinutes;
    private final AttendanceStatus attendanceStatus;
    private final String summary;
    private final String privateNotes;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public LessonSessionEntity(
            UUID id,
            UUID studentProgramId,
            UUID teacherId,
            Instant startedAt,
            int durationMinutes,
            AttendanceStatus attendanceStatus,
            String summary,
            String privateNotes
    ) {
        this(id, studentProgramId, teacherId, startedAt, durationMinutes, attendanceStatus,
                summary, privateNotes, null, null, null);
    }

    public LessonSessionEntity(
            UUID id,
            UUID studentProgramId,
            UUID teacherId,
            Instant startedAt,
            int durationMinutes,
            AttendanceStatus attendanceStatus,
            String summary,
            String privateNotes,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.teacherId = Objects.requireNonNull(teacherId);
        this.startedAt = Objects.requireNonNull(startedAt);
        if (durationMinutes < 1 || durationMinutes > 600) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and 600");
        }
        this.durationMinutes = durationMinutes;
        this.attendanceStatus = Objects.requireNonNull(attendanceStatus);
        this.summary = summary;
        this.privateNotes = privateNotes;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentProgramId() { return studentProgramId; }
    public UUID getTeacherId() { return teacherId; }
    public Instant getStartedAt() { return startedAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public String getSummary() { return summary; }
    public String getPrivateNotes() { return privateNotes; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
