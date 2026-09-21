package com.tutorplatform.content.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderLessonMaterialsRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<@NotNull UUID> orderedIds) {}
