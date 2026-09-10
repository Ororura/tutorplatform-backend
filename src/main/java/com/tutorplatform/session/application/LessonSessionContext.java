package com.tutorplatform.session.application;

import java.util.UUID;

public record LessonSessionContext(
    UUID sessionId,
    UUID teacherId,
    UUID studentProgramId,
    UUID studentId
) {
}
