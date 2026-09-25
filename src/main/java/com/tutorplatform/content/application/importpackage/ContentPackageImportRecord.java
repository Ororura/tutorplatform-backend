package com.tutorplatform.content.application.importpackage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentPackageImportRecord(
        UUID id,
        UUID teacherId,
        UUID learningProgramId,
        UUID confirmationId,
        String packageDigest,
        int moduleCount,
        int topicCount,
        int materialCount,
        List<UUID> createdModuleIds,
        Instant createdAt) {

    public ContentPackageImportRecord {
        createdModuleIds = List.copyOf(createdModuleIds);
    }
}
