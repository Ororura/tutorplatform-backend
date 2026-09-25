package com.tutorplatform.content.application.importpackage;

import java.util.List;
import java.util.UUID;

public record ContentPackageImportResult(
        UUID programId,
        UUID confirmationId,
        String digest,
        int moduleCount,
        int topicCount,
        int materialCount,
        List<UUID> createdModuleIds) {
    public ContentPackageImportResult {
        createdModuleIds = List.copyOf(createdModuleIds);
    }

    static ContentPackageImportResult fromRecord(ContentPackageImportRecord record) {
        return new ContentPackageImportResult(
                record.learningProgramId(),
                record.confirmationId(),
                record.packageDigest(),
                record.moduleCount(),
                record.topicCount(),
                record.materialCount(),
                record.createdModuleIds());
    }
}
