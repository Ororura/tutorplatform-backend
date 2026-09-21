package com.tutorplatform.submission.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Student source code for a CODE homework or topic practice task")
public record SubmitCodeAnswerRequest(
        @Schema(format = "uuid") UUID homeworkItemId,
        @Schema(format = "uuid") UUID studentProgramId,
        @Schema(format = "uuid") UUID topicId,
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String sourceCode) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Unknown request property: " + property);
    }
}
