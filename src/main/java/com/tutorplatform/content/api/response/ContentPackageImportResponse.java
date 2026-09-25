package com.tutorplatform.content.api.response;

import com.tutorplatform.content.application.importpackage.ContentPackageImportResult;
import java.util.List;
import java.util.UUID;

public record ContentPackageImportResponse(
        UUID programId,
        UUID confirmationId,
        String digest,
        int moduleCount,
        int topicCount,
        int materialCount,
        List<UUID> createdModuleIds) {
    public static ContentPackageImportResponse from(ContentPackageImportResult result) {
        return new ContentPackageImportResponse(
                result.programId(),
                result.confirmationId(),
                result.digest(),
                result.moduleCount(),
                result.topicCount(),
                result.materialCount(),
                result.createdModuleIds());
    }
}
