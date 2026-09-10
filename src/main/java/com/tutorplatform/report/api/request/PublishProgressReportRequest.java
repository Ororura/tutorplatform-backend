package com.tutorplatform.report.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(name = "PublishProgressReportRequest")
public record PublishProgressReportRequest(
    @NotNull @PositiveOrZero
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    Long version
) {
}
