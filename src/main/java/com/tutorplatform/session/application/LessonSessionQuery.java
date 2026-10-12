package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.AttendanceStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LessonSessionQuery {

    Optional<LessonSessionContext> findContextById(UUID lessonSessionId);

    Optional<LessonSessionContext> findContextByIdForUpdate(UUID lessonSessionId);

    LessonSessionPage findPageByTeacherAndStudent(
            UUID teacherId,
            UUID studentId,
            int page,
            int size,
            String sortField,
            boolean ascending,
            AttendanceStatus attendanceStatus,
            Instant startedBefore);
}
