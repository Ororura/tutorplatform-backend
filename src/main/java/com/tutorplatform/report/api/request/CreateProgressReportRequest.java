package com.tutorplatform.report.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "CreateProgressReportRequest")
public record CreateProgressReportRequest(
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID studentProgramId,
    @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
    UUID learningPeriodId
) {
}
