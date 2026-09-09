package com.tutorplatform.submission.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SubmissionEntity {

    private final UUID id;
    private final UUID studentId;
    private final UUID studentProgramId;
    private final UUID taskId;
    private final UUID homeworkItemId;
    private final int attemptNo;
    private final String textAnswer;
    private final Instant submittedAt;
    private final Instant createdAt;
    private SubmissionStatus status;

    public SubmissionEntity(
        UUID id,
        UUID studentId,
        UUID studentProgramId,
        UUID taskId,
        UUID homeworkItemId,
        int attemptNo,
        SubmissionStatus status,
        String textAnswer,
        Instant submittedAt
    ) {
        this(id, studentId, studentProgramId, taskId, homeworkItemId, attemptNo, status,
            textAnswer, submittedAt, null);
    }

    public SubmissionEntity(
        UUID id,
        UUID studentId,
        UUID studentProgramId,
        UUID taskId,
        UUID homeworkItemId,
        int attemptNo,
        SubmissionStatus status,
        String textAnswer,
        Instant submittedAt,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.studentId = Objects.requireNonNull(studentId);
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.taskId = Objects.requireNonNull(taskId);
        this.homeworkItemId = homeworkItemId;
        if (attemptNo <= 0) {
            throw new IllegalArgumentException("Submission attempt number must be positive");
        }
        this.attemptNo = attemptNo;
        this.status = Objects.requireNonNull(status);
        this.textAnswer = textAnswer;
        this.submittedAt = Objects.requireNonNull(submittedAt);
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getStudentProgramId() {
        return studentProgramId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getHomeworkItemId() {
        return homeworkItemId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void review(SubmissionStatus reviewStatus) {
        if (status != SubmissionStatus.NEEDS_REVIEW) {
            throw new IllegalStateException("Submission is not awaiting review");
        }
        if (reviewStatus != SubmissionStatus.PASSED && reviewStatus != SubmissionStatus.FAILED) {
            throw new IllegalArgumentException("Review status must be PASSED or FAILED");
        }
        status = reviewStatus;
    }
}
