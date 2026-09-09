package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;

import java.util.Optional;
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

    StudentHomeworkPage findPageByStudent(
        UUID studentId,
        UUID studentProgramId,
        HomeworkStatus status,
        int page,
        int size,
        String sortField,
        boolean ascending
    );

    Optional<StudentHomeworkDetails> findDetailsByStudent(UUID studentId, UUID homeworkId);

    Optional<HomeworkSubmissionContext> findSubmissionContext(UUID homeworkItemId);

    record HomeworkSubmissionContext(
        UUID homeworkId,
        UUID studentProgramId,
        UUID assignedByTeacherId,
        HomeworkStatus homeworkStatus,
        UUID homeworkItemId,
        UUID taskId
    ) {
    }
}
