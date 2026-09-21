package com.tutorplatform.content.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LessonMaterialEntity {

    private final UUID id;
    private final UUID topicId;
    private final UUID createdByTeacherId;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private LessonMaterialType materialType;
    private String title;
    private String content;
    private UUID fileAssetId;
    private String externalUrl;
    private int position;

    public LessonMaterialEntity(
            UUID id,
            UUID topicId,
            UUID createdByTeacherId,
            LessonMaterialType materialType,
            String title,
            String content,
            UUID fileAssetId,
            String externalUrl,
            int position) {
        this(
                id,
                topicId,
                createdByTeacherId,
                materialType,
                title,
                content,
                fileAssetId,
                externalUrl,
                position,
                null,
                null,
                null);
    }

    public LessonMaterialEntity(
            UUID id,
            UUID topicId,
            UUID createdByTeacherId,
            LessonMaterialType materialType,
            String title,
            String content,
            UUID fileAssetId,
            String externalUrl,
            int position,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.topicId = Objects.requireNonNull(topicId);
        this.createdByTeacherId = Objects.requireNonNull(createdByTeacherId);
        applyChanges(materialType, title, content, fileAssetId, externalUrl, position);
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            LessonMaterialType materialType,
            String title,
            String content,
            UUID fileAssetId,
            String externalUrl,
            int position) {
        applyChanges(materialType, title, content, fileAssetId, externalUrl, position);
    }

    private void applyChanges(
            LessonMaterialType materialType,
            String title,
            String content,
            UUID fileAssetId,
            String externalUrl,
            int position) {
        LessonMaterialType requiredType = Objects.requireNonNull(materialType);
        if (position < 0) {
            throw new IllegalArgumentException("position must be greater than or equal to 0");
        }
        if (requiresContent(requiredType) && content == null) {
            throw new IllegalArgumentException(requiredType + " material requires content");
        }
        if (requiresContent(requiredType) && (fileAssetId != null || externalUrl != null)) {
            throw new IllegalArgumentException(requiredType + " material only supports content");
        }
        if (requiresFileAsset(requiredType) && fileAssetId == null) {
            throw new IllegalArgumentException(requiredType + " material requires fileAssetId");
        }
        if (requiredType == LessonMaterialType.LINK && externalUrl == null) {
            throw new IllegalArgumentException("LINK material requires externalUrl");
        }
        if (requiredType == LessonMaterialType.LINK && (content != null || fileAssetId != null)) {
            throw new IllegalArgumentException("LINK material only supports externalUrl");
        }
        this.materialType = requiredType;
        this.title = Objects.requireNonNull(title);
        this.content = content;
        this.fileAssetId = fileAssetId;
        this.externalUrl = externalUrl;
        this.position = position;
    }

    private boolean requiresContent(LessonMaterialType type) {
        return type == LessonMaterialType.MARKDOWN
                || type == LessonMaterialType.TEXT
                || type == LessonMaterialType.CODE_EXAMPLE;
    }

    private boolean requiresFileAsset(LessonMaterialType type) {
        return type == LessonMaterialType.IMAGE || type == LessonMaterialType.FILE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public UUID getCreatedByTeacherId() {
        return createdByTeacherId;
    }

    public LessonMaterialType getMaterialType() {
        return materialType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public UUID getFileAssetId() {
        return fileAssetId;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public int getPosition() {
        return position;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
