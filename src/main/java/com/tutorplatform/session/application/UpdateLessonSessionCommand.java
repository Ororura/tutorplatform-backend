package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.AttendanceStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record UpdateLessonSessionCommand(
    Instant startedAt,
    int durationMinutes,
    AttendanceStatus attendanceStatus,
    String summary,
    String privateNotes,
    long version,
    List<LessonSessionTopicInput> topics
) {
    public UpdateLessonSessionCommand {
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(attendanceStatus);
        if (version < 0) {
            throw new IllegalArgumentException("version must be greater than or equal to 0");
        }
        topics = List.copyOf(Objects.requireNonNull(topics));
    }
}
