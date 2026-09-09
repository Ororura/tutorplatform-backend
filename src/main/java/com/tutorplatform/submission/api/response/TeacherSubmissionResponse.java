package com.tutorplatform.submission.api.response;

import com.tutorplatform.submission.application.SubmissionResult;
import com.tutorplatform.submission.domain.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TeacherSubmissionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID studentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID taskId,
        @Schema(nullable = true, format = "uuid") UUID homeworkItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") int attemptNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SubmissionStatus status,
        @Schema(nullable = true) String textAnswer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant submittedAt
) {
    public static TeacherSubmissionResponse from(SubmissionResult result) {
        return new TeacherSubmissionResponse(
                result.id(), result.studentId(), result.taskId(), result.homeworkItemId(),
                result.attemptNo(), result.status(), result.textAnswer(), result.submittedAt()
        );
    }
}
