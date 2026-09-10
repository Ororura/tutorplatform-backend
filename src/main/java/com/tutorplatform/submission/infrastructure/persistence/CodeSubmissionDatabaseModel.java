package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.submission.domain.CodeSubmissionEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "code_submissions")
class CodeSubmissionDatabaseModel {

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "source_code", nullable = false, updatable = false)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 24)
    private CodeExecutionStatus executionStatus;

    @Column(name = "passed_tests", nullable = false)
    private int passedTests;

    @Column(name = "total_tests", nullable = false)
    private int totalTests;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "stdout_excerpt")
    private String stdoutExcerpt;

    @Column(name = "stderr_excerpt")
    private String stderrExcerpt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CodeSubmissionDatabaseModel() {
    }

    CodeSubmissionDatabaseModel(CodeSubmissionEntity entity) {
        submissionId = Objects.requireNonNull(entity.getSubmissionId());
        sourceCode = Objects.requireNonNull(entity.getSourceCode());
        executionStatus = Objects.requireNonNull(entity.getExecutionStatus());
        passedTests = entity.getPassedTests();
        totalTests = entity.getTotalTests();
        executionTimeMs = entity.getExecutionTimeMs();
        stdoutExcerpt = entity.getStdoutExcerpt();
        stderrExcerpt = entity.getStderrExcerpt();
        updatedAt = entity.getUpdatedAt();
    }

    CodeSubmissionEntity toEntity() {
        return new CodeSubmissionEntity(
            submissionId, sourceCode, executionStatus, passedTests, totalTests,
            executionTimeMs, stdoutExcerpt, stderrExcerpt, updatedAt
        );
    }
}
