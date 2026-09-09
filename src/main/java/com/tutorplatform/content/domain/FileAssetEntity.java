package com.tutorplatform.content.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FileAssetEntity(UUID id, UUID uploadedByTeacherId, StorageProvider storageProvider, String storageKey,
                              String originalFilename, String mimeType, long sizeBytes, String sha256,
                              Instant createdAt) {

    public FileAssetEntity(
        UUID id,
        UUID uploadedByTeacherId,
        StorageProvider storageProvider,
        String storageKey,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        String sha256
    ) {
        this(id, uploadedByTeacherId, storageProvider, storageKey, originalFilename, mimeType,
            sizeBytes, sha256, null);
    }

    public FileAssetEntity(
        UUID id,
        UUID uploadedByTeacherId,
        StorageProvider storageProvider,
        String storageKey,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        String sha256,
        Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.uploadedByTeacherId = Objects.requireNonNull(uploadedByTeacherId);
        this.storageProvider = Objects.requireNonNull(storageProvider);
        this.storageKey = Objects.requireNonNull(storageKey);
        this.originalFilename = Objects.requireNonNull(originalFilename);
        this.mimeType = Objects.requireNonNull(mimeType);
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be greater than or equal to 0");
        }
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }
}
