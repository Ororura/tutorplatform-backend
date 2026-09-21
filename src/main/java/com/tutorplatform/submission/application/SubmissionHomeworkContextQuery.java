package com.tutorplatform.submission.application;

import java.util.Optional;
import java.util.UUID;

/** Submission-owned port for validating an optional homework submission context. */
public interface SubmissionHomeworkContextQuery {

    Optional<HomeworkSubmissionContext> findSubmissionContext(UUID homeworkItemId);

    record HomeworkSubmissionContext(
            UUID homeworkId,
            UUID studentProgramId,
            UUID assignedByTeacherId,
            boolean cancelled,
            boolean completed,
            UUID homeworkItemId,
            UUID taskId) {
        public HomeworkSubmissionContext(
                UUID homeworkId,
                UUID studentProgramId,
                UUID assignedByTeacherId,
                boolean cancelled,
                UUID homeworkItemId,
                UUID taskId) {
            this(
                    homeworkId,
                    studentProgramId,
                    assignedByTeacherId,
                    cancelled,
                    false,
                    homeworkItemId,
                    taskId);
        }
    }
}
