package com.tutorplatform.content.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FileAssetEntity {

    private final UUID id;
    private final UUID uploadedByTeacherId;
    private final StorageProvider storageProvider;
    private final String storageKey;
    private final String originalFilename;
    private final String mimeType;
    private final long sizeBytes;
    private final String sha256;
    private final Instant createdAt;

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

    public UUID getId() { return id; }
    public UUID getUploadedByTeacherId() { return uploadedByTeacherId; }
    public StorageProvider getStorageProvider() { return storageProvider; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public Instant getCreatedAt() { return createdAt; }
}
