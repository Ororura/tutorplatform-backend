package com.tutorplatform.student.api.management;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record StudentRelationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RelationType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant startedAt) {
    public enum RelationType {
        PRIMARY
    }
}
