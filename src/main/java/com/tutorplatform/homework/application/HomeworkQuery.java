package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;

import java.util.UUID;

public interface HomeworkQuery {

    HomeworkPage findPageByTeacherAndStudent(
            UUID teacherId,
            UUID studentId,
            UUID studentProgramId,
            HomeworkStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending
    );
}
