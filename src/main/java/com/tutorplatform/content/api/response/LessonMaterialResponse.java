package com.tutorplatform.content.api.response;

import com.tutorplatform.content.application.LessonMaterialResult;
import com.tutorplatform.content.domain.LessonMaterialType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record LessonMaterialResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID topicId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LessonMaterialType materialType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200) String title,
        @Schema(nullable = true) String content,
        @Schema(nullable = true) String externalUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
                Instant updatedAt) {
    public static LessonMaterialResponse from(LessonMaterialResult material) {
        return new LessonMaterialResponse(
                material.id(),
                material.topicId(),
                material.materialType(),
                material.title(),
                material.content(),
                material.externalUrl(),
                material.position(),
                material.version(),
                material.createdAt(),
                material.updatedAt());
    }
}
