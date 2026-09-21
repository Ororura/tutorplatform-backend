package com.tutorplatform.submission.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CodeSubmissionEntity {

    private final UUID submissionId;
    private final String sourceCode;
    private CodeExecutionStatus executionStatus;
    private int passedTests;
    private int totalTests;
    private Integer executionTimeMs;
    private String stdoutExcerpt;
    private String stderrExcerpt;
    private Instant updatedAt;

    public CodeSubmissionEntity(UUID submissionId, String sourceCode, int totalTests) {
        this(
                submissionId,
                sourceCode,
                CodeExecutionStatus.PENDING,
                0,
                totalTests,
                null,
                null,
                null,
                null);
    }

    public CodeSubmissionEntity(
            UUID submissionId,
            String sourceCode,
            CodeExecutionStatus executionStatus,
            int passedTests,
            int totalTests,
            Integer executionTimeMs,
            String stdoutExcerpt,
            String stderrExcerpt,
            Instant updatedAt) {
        this.submissionId = Objects.requireNonNull(submissionId);
        this.sourceCode = Objects.requireNonNull(sourceCode);
        this.executionStatus = Objects.requireNonNull(executionStatus);
        validateCounts(passedTests, totalTests);
        if (executionTimeMs != null && executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs must not be negative");
        }
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        this.executionTimeMs = executionTimeMs;
        this.stdoutExcerpt = stdoutExcerpt;
        this.stderrExcerpt = stderrExcerpt;
        this.updatedAt = updatedAt;
    }

    public void complete(
            CodeExecutionStatus status,
            int passedTests,
            int totalTests,
            long executionTimeMs,
            String stdoutExcerpt,
            String stderrExcerpt) {
        if (status == CodeExecutionStatus.PENDING || status == CodeExecutionStatus.RUNNING) {
            throw new IllegalArgumentException("A final execution status is required");
        }
        validateCounts(passedTests, totalTests);
        if (executionTimeMs < 0) {
            throw new IllegalArgumentException("executionTimeMs must not be negative");
        }
        this.executionStatus = status;
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        this.executionTimeMs = Math.toIntExact(executionTimeMs);
        this.stdoutExcerpt = stdoutExcerpt;
        this.stderrExcerpt = stderrExcerpt;
    }

    private static void validateCounts(int passedTests, int totalTests) {
        if (passedTests < 0 || totalTests < 0 || passedTests > totalTests) {
            throw new IllegalArgumentException("test counts are invalid");
        }
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public CodeExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getStdoutExcerpt() {
        return stdoutExcerpt;
    }

    public String getStderrExcerpt() {
        return stderrExcerpt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
