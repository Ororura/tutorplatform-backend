package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record LessonSessionChangedEvent(
    UUID lessonSessionId,
    UUID studentProgramId,
    AttendanceStatus attendanceStatus,
    Instant startedAt,
    int durationMinutes,
    AttendanceStatus previousAttendanceStatus,
    Instant previousStartedAt,
    Integer previousDurationMinutes
) {
    public boolean affectsLearningMinutes() {
        if (previousAttendanceStatus == null) {
            return attendanceStatus == AttendanceStatus.ATTENDED;
        }
        boolean learningFactsChanged = attendanceStatus != previousAttendanceStatus
            || durationMinutes != previousDurationMinutes
            || !startedAt.equals(previousStartedAt);
        return learningFactsChanged && (attendanceStatus == AttendanceStatus.ATTENDED
            || previousAttendanceStatus == AttendanceStatus.ATTENDED);
    }
}
