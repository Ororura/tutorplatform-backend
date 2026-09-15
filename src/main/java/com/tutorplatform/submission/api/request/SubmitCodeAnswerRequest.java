package com.tutorplatform.submission.api.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Student source code for a CODE homework task")
public class SubmitCodeAnswerRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    private final UUID homeworkItemId;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String sourceCode;

    private final Map<String, Object> unknownProperties = new LinkedHashMap<>();

    @JsonCreator
    public SubmitCodeAnswerRequest(
        @JsonProperty("homeworkItemId") UUID homeworkItemId,
        @JsonProperty("sourceCode") String sourceCode
    ) {
        this.homeworkItemId = homeworkItemId;
        this.sourceCode = sourceCode;
    }

    @JsonAnySetter
    public void collectUnknownProperty(String property, Object value) {
        unknownProperties.put(property, value);
    }

    public UUID homeworkItemId() { return homeworkItemId; }
    public String sourceCode() { return sourceCode; }

    @Schema(hidden = true)
    public Map<String, Object> unknownProperties() {
        return Map.copyOf(unknownProperties);
    }
}
