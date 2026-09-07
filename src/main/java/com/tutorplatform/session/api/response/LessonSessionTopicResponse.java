package com.tutorplatform.session.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tutorplatform.session.application.LessonSessionTopicResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record LessonSessionTopicResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID topicId,
    @JsonProperty("isPrimary")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean primary
) {
    static LessonSessionTopicResponse from(LessonSessionTopicResult topic) {
        return new LessonSessionTopicResponse(topic.topicId(), topic.primary());
    }
}
