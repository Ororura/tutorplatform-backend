package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.AttendanceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LessonSessionResult(
    UUID id,
    UUID studentProgramId,
    UUID teacherId,
    Instant startedAt,
    int durationMinutes,
    AttendanceStatus attendanceStatus,
    String summary,
    String privateNotes,
    long version,
    Instant createdAt,
    Instant updatedAt,
    List<LessonSessionTopicResult> topics
) {
}
