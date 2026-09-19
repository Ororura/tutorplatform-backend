package com.tutorplatform.program.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderLearningProgramModulesRequest(
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<@NotNull UUID> orderedIds
) {
}
