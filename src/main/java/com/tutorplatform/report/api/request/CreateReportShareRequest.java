package com.tutorplatform.report.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record CreateReportShareRequest(
    @Schema(types = {"string", "null"}, format = "date-time") Instant expiresAt
) {
}
