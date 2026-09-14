package com.tutorplatform.program.api;

import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ProgramTopicResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(nullable = true) String description,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicStatus topicStatus,
    @Schema(nullable = true) StudentTopicProgressStatus progressStatus
) {
}
