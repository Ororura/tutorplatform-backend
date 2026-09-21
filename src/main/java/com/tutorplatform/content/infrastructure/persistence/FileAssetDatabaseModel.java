package com.tutorplatform.content.infrastructure.persistence;

import com.tutorplatform.content.domain.FileAssetEntity;
import com.tutorplatform.content.domain.StorageProvider;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "file_assets")
public class FileAssetDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by_teacher_id",
            nullable = false,
            insertable = false,
            updatable = false)
    private TeacherDatabaseModel uploadedByTeacher;

    @Column(name = "uploaded_by_teacher_id", nullable = false)
    private UUID uploadedByTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 24)
    private StorageProvider storageProvider;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 160)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(length = 64)
    private String sha256;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FileAssetDatabaseModel() {}

    FileAssetDatabaseModel(FileAssetEntity fileAsset) {
        id = Objects.requireNonNull(fileAsset.id());
        uploadedByTeacherId = Objects.requireNonNull(fileAsset.uploadedByTeacherId());
        storageProvider = Objects.requireNonNull(fileAsset.storageProvider());
        storageKey = Objects.requireNonNull(fileAsset.storageKey());
        originalFilename = Objects.requireNonNull(fileAsset.originalFilename());
        mimeType = Objects.requireNonNull(fileAsset.mimeType());
        sizeBytes = fileAsset.sizeBytes();
        sha256 = fileAsset.sha256();
        createdAt = fileAsset.createdAt();
    }

    FileAssetEntity toEntity() {
        return new FileAssetEntity(
                id,
                uploadedByTeacherId,
                storageProvider,
                storageKey,
                originalFilename,
                mimeType,
                sizeBytes,
                sha256,
                createdAt);
    }
}
