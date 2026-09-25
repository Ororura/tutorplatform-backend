package com.tutorplatform.content.application.importpackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPackageImportRepository {

    Optional<ContentPackageImportRecord> findByConfirmation(
            UUID teacherId, UUID learningProgramId, UUID confirmationId);

    /**
     * Serializes attempts for this confirmation until the caller's transaction commits or rolls
     * back. The caller must create the imported content and save the result in that same
     * transaction. An existing result can be compared with the requested package digest before
     * replaying it.
     */
    Optional<ContentPackageImportRecord> registerConfirmation(
            UUID teacherId, UUID learningProgramId, UUID confirmationId);

    ContentPackageImportRecord saveSuccessfulImport(
            UUID teacherId,
            UUID learningProgramId,
            UUID confirmationId,
            String packageDigest,
            int moduleCount,
            int topicCount,
            int materialCount,
            List<UUID> createdModuleIds);
}
