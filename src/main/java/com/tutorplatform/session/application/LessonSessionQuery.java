package com.tutorplatform.session.application;

import java.util.UUID;

public interface LessonSessionQuery {

    LessonSessionPage findPageByTeacherAndStudent(
        UUID teacherId,
        UUID studentId,
        int page,
        int size,
        String sortField,
        boolean ascending
    );
}
