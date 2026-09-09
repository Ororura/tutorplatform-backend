package com.tutorplatform.submission.api.request;

import com.tutorplatform.submission.domain.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReviewTextSubmissionRequest(
        @NotNull
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PASSED", "FAILED"}
        )
        SubmissionStatus status
) {
}
