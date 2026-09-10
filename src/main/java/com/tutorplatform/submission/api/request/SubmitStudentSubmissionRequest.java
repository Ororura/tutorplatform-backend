package com.tutorplatform.submission.api.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A task-type-dispatched submission request. Exactly one answer field is accepted by the
 * application service; task configuration and execution data always remain server-owned.
 */
public class SubmitStudentSubmissionRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    private final UUID homeworkItemId;

    @Schema(nullable = true, description = "Answer for a TEXT task")
    private final String textAnswer;

    @Schema(nullable = true, description = "Source code for a CODE task")
    private final String sourceCode;

    private final Map<String, Object> unknownProperties = new LinkedHashMap<>();

    @JsonCreator
    public SubmitStudentSubmissionRequest(
        @JsonProperty("homeworkItemId") UUID homeworkItemId,
        @JsonProperty("textAnswer") String textAnswer,
        @JsonProperty("sourceCode") String sourceCode
    ) {
        this.homeworkItemId = homeworkItemId;
        this.textAnswer = textAnswer;
        this.sourceCode = sourceCode;
    }

    @JsonAnySetter
    public void collectUnknownProperty(String property, Object value) {
        unknownProperties.put(property, value);
    }

    public UUID homeworkItemId() { return homeworkItemId; }
    public String textAnswer() { return textAnswer; }
    public String sourceCode() { return sourceCode; }

    @Schema(hidden = true)
    public Map<String, Object> unknownProperties() {
        return Map.copyOf(unknownProperties);
    }
}
