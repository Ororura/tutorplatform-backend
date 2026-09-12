package com.tutorplatform.student.api.codeexecution;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "RunCodeRequest", description = "Transient code run in an assigned homework item context")
public record RunCodeRequest(
    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String sourceCode,
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID homeworkItemId
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("Unknown request property: " + property);
    }
}
