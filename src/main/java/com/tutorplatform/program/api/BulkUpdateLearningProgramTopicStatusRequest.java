package com.tutorplatform.program.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tutorplatform.program.domain.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record BulkUpdateLearningProgramTopicStatusRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicStatus status,
        @NotEmpty @Size(max = 375) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<@NotNull @Valid BulkUpdateLearningProgramTopicStatusItem> topics) {

    @AssertTrue(message = "Topic IDs must be unique")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTopicIdsUnique() {
        if (topics == null) {
            return true;
        }
        HashSet<UUID> ids = new HashSet<>();
        for (BulkUpdateLearningProgramTopicStatusItem topic : topics) {
            if (topic != null && topic.id() != null && !ids.add(topic.id())) {
                return false;
            }
        }
        return true;
    }
}
