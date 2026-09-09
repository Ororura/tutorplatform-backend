package com.tutorplatform.submission.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitTextAnswerRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID homeworkItemId,

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String textAnswer
) {
}
