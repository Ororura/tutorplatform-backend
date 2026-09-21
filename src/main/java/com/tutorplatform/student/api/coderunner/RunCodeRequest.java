package com.tutorplatform.student.api.coderunner;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(
        name = "RunCodeRequest",
        description = "Transient code run in an assigned homework or topic context")
public record RunCodeRequest(
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String sourceCode,
        @Schema(format = "uuid") UUID homeworkItemId,
        @Schema(format = "uuid") UUID studentProgramId,
        @Schema(format = "uuid") UUID topicId) {
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Unknown request property: " + property);
    }
}
