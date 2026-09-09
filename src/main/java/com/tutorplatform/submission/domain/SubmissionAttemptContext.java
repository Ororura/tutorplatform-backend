package com.tutorplatform.submission.domain;

import java.util.Objects;
import java.util.UUID;

public record SubmissionAttemptContext(
        UUID studentId,
        UUID studentProgramId,
        UUID taskId,
        UUID homeworkItemId
) {
    public SubmissionAttemptContext {
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(studentProgramId);
        Objects.requireNonNull(taskId);
    }
}
