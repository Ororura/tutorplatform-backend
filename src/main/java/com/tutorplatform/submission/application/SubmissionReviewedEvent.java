package com.tutorplatform.submission.application;

import com.tutorplatform.submission.domain.SubmissionStatus;
import java.util.UUID;

public record SubmissionReviewedEvent(
        UUID submissionId,
        UUID studentId,
        UUID studentProgramId,
        UUID taskId,
        UUID homeworkItemId,
        SubmissionStatus resultingStatus) {}
