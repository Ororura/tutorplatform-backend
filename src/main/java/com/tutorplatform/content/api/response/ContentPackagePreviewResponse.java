package com.tutorplatform.content.api.response;

import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record ContentPackagePreviewResponse(
        boolean valid,
        UUID programId,
        String digest,
        int moduleCount,
        int topicCount,
        int materialCount,
        List<ContentPackagePreviewResult.Module> modules,
        List<ContentPackagePreviewError> errors) {
    public static ContentPackagePreviewResponse from(ContentPackagePreviewResult result) {
        return new ContentPackagePreviewResponse(
                true,
                result.programId(),
                result.sha256Digest(),
                result.moduleCount(),
                result.topicCount(),
                result.materialCount(),
                result.modules(),
                List.of());
    }

    @Schema(description = "Individual YAML parser or content validation error")
    public record ContentPackagePreviewError(String code, String path, String message) {}
}
