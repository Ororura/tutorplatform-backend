package com.tutorplatform.submission.application;

import com.tutorplatform.submission.domain.CodeSubmissionEntity;
import com.tutorplatform.submission.domain.CodeSubmissionRepository;
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
        Instant submittedAt,
        CodeSubmissionResult execution,
        String reviewComment) {
    public SubmissionResult(
            UUID id,
            UUID studentId,
            UUID taskId,
            UUID homeworkItemId,
            int attemptNo,
            SubmissionStatus status,
            String textAnswer,
            Instant submittedAt,
            CodeSubmissionResult execution) {
        this(
                id,
                studentId,
                taskId,
                homeworkItemId,
                attemptNo,
                status,
                textAnswer,
                submittedAt,
                execution,
                null);
    }

    static SubmissionResult from(SubmissionEntity submission) {
        return new SubmissionResult(
                submission.getId(),
                submission.getStudentId(),
                submission.getTaskId(),
                submission.getHomeworkItemId(),
                submission.getAttemptNo(),
                submission.getStatus(),
                submission.getTextAnswer(),
                submission.getSubmittedAt(),
                null,
                submission.getReviewComment());
    }

    SubmissionResult withCodeSubmission(
            CodeSubmissionEntity codeSubmission, boolean includeSourceCode) {
        return new SubmissionResult(
                id,
                studentId,
                taskId,
                homeworkItemId,
                attemptNo,
                status,
                textAnswer,
                submittedAt,
                CodeSubmissionResult.from(codeSubmission, includeSourceCode),
                reviewComment);
    }

    SubmissionResult withCodeSubmission(CodeSubmissionRepository.Summary summary) {
        return new SubmissionResult(
                id,
                studentId,
                taskId,
                homeworkItemId,
                attemptNo,
                status,
                textAnswer,
                submittedAt,
                CodeSubmissionResult.from(summary),
                reviewComment);
    }
}
