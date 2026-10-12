package com.tutorplatform.dashboard.api;

import com.tutorplatform.dashboard.application.TeacherDashboardAttentionPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TeacherDashboardAttentionPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<TeacherDashboardResponse.AttentionItem> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "100")
                int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") long totalElements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int totalPages) {
    static TeacherDashboardAttentionPageResponse from(TeacherDashboardAttentionPage result) {
        return new TeacherDashboardAttentionPageResponse(
                result.items().stream().map(TeacherDashboardResponse.AttentionItem::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
