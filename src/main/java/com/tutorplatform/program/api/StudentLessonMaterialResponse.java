package com.tutorplatform.program.api;

import com.tutorplatform.content.application.LessonMaterialResult;
import com.tutorplatform.content.domain.LessonMaterialType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record StudentLessonMaterialResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LessonMaterialType materialType,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200) String title,
    @Schema(nullable = true) String content,
    @Schema(nullable = true) String externalUrl,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int position
) {
    public static StudentLessonMaterialResponse from(LessonMaterialResult material) {
        return new StudentLessonMaterialResponse(
            material.id(),
            material.materialType(),
            material.title(),
            material.content(),
            material.externalUrl(),
            material.position()
        );
    }
}
