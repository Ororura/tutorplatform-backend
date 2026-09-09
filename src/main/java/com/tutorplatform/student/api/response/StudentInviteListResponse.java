package com.tutorplatform.student.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record StudentInviteListResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<StudentInviteSummaryResponse> items
) {
}
