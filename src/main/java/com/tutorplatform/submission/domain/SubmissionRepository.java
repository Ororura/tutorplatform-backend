package com.tutorplatform.submission.domain;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository {

    SubmissionEntity saveAndFlush(SubmissionEntity submission);

    Optional<SubmissionEntity> findById(UUID submissionId);

    Optional<SubmissionEntity> findByIdAndStudentId(UUID submissionId, UUID studentId);

    SubmissionPage findPageByStudentId(UUID studentId, int page, int size);

    SubmissionPage findPageByStudentIdAndTaskId(UUID studentId, UUID taskId, int page, int size);

    SubmissionPage findPageForTeacher(
            UUID studentId,
            SubmissionStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending
    );

    SubmissionPage findAttempts(SubmissionAttemptContext context, int page, int size);

    Optional<SubmissionEntity> findLatestAttempt(SubmissionAttemptContext context);

    boolean existsByStatus(SubmissionAttemptContext context, SubmissionStatus status);

    /**
     * Locks the StudentProgram row and returns the next number for this exact V007 attempt context.
     * The caller must keep the same transaction open until {@link #saveAndFlush(SubmissionEntity)}.
     */
    int nextAttemptNo(SubmissionAttemptContext context);
}
