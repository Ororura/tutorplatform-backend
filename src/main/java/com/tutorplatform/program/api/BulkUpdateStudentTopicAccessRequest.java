package com.tutorplatform.program.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public record BulkUpdateStudentTopicAccessRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                StudentTopicAccessStatus status,
        @NotEmpty @Size(min = 1, max = 375) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<@NotNull UUID> topicIds) {

    @AssertTrue(message = "Topic IDs must be unique")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTopicIdsUnique() {
        if (topicIds == null) {
            return true;
        }

        return new HashSet<>(topicIds).size() == topicIds.size();
    }
}
