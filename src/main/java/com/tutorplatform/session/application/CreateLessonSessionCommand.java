package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.AttendanceStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateLessonSessionCommand(
    UUID studentId,
    UUID studentProgramId,
    Instant startedAt,
    int durationMinutes,
    AttendanceStatus attendanceStatus,
    String summary,
    String privateNotes,
    List<LessonSessionTopicInput> topics
) {
    public CreateLessonSessionCommand {
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(studentProgramId);
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(attendanceStatus);
        topics = List.copyOf(Objects.requireNonNull(topics));
    }
}
