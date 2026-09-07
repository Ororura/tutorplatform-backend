package com.tutorplatform.content.api.request;

import com.tutorplatform.content.domain.LessonMaterialType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateLessonMaterialRequest(
    @NotNull
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"MARKDOWN", "TEXT", "CODE_EXAMPLE", "LINK"}
    )
    LessonMaterialType materialType,

    @NotBlank
    @Size(max = 200)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    String title,

    @Schema(nullable = true)
    String content,

    @Schema(nullable = true)
    String externalUrl,

    @PositiveOrZero
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    int position,

    @NotNull
    @PositiveOrZero
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    Long version
) {
}
