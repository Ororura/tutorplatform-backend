package com.tutorplatform.session.application;

import java.util.List;
import java.util.UUID;

public interface LearningSessionQuery {
    List<AttendedLessonSession> findAttendedByStudentProgram(UUID studentProgramId);
}
