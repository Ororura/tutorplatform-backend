package com.tutorplatform.submission.api.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Student source code for a CODE homework task")
public record SubmitCodeAnswerRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID homeworkItemId,
    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String sourceCode
) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Unknown request property: " + property);
    }
}
