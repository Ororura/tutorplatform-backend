package com.tutorplatform.session.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LessonSessionTopicRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID topicId,
        @JsonProperty("isPrimary")
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = "false")
                boolean primary) {}
