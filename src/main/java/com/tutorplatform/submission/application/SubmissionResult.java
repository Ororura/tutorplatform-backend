package com.tutorplatform.submission.application;

import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResult(
        UUID id,
        UUID studentId,
        UUID taskId,
        UUID homeworkItemId,
        int attemptNo,
        SubmissionStatus status,
        String textAnswer,
        Instant submittedAt
) {
    static SubmissionResult from(SubmissionEntity submission) {
        return new SubmissionResult(
                submission.getId(), submission.getStudentId(), submission.getTaskId(),
                submission.getHomeworkItemId(),
                submission.getAttemptNo(), submission.getStatus(), submission.getTextAnswer(),
                submission.getSubmittedAt()
        );
    }
}
