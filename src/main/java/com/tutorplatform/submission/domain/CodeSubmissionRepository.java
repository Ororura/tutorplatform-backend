package com.tutorplatform.submission.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CodeSubmissionRepository {

    CodeSubmissionEntity saveAndFlush(CodeSubmissionEntity codeSubmission);

    Optional<CodeSubmissionEntity> findBySubmissionId(UUID submissionId);

    Map<UUID, Summary> findSummaries(Collection<UUID> submissionIds);

    record Summary(
        UUID submissionId,
        CodeExecutionStatus executionStatus,
        int passedTests,
        int totalTests,
        Integer executionTimeMs,
        String stdoutExcerpt,
        String stderrExcerpt
    ) {
    }
}
