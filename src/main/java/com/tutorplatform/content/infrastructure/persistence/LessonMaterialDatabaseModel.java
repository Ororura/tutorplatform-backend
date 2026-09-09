package com.tutorplatform.content.infrastructure.persistence;

import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.infrastructure.persistence.TopicDatabaseModel;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "lesson_materials")
public class LessonMaterialDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, insertable = false, updatable = false)
    private TopicDatabaseModel topic;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel createdByTeacher;

    @Column(name = "created_by_teacher_id", nullable = false)
    private UUID createdByTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 24)
    private LessonMaterialType materialType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_asset_id", insertable = false, updatable = false)
    private FileAssetDatabaseModel fileAsset;

    @Column(name = "file_asset_id")
    private UUID fileAssetId;

    @Column(name = "external_url", columnDefinition = "text")
    private String externalUrl;

    @Column(nullable = false)
    private int position;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LessonMaterialDatabaseModel() {
    }

    LessonMaterialDatabaseModel(LessonMaterialEntity lessonMaterial) {
        id = Objects.requireNonNull(lessonMaterial.getId());
        updateFrom(lessonMaterial);
        version = lessonMaterial.getVersion();
    }

    void updateFrom(LessonMaterialEntity lessonMaterial) {
        topicId = Objects.requireNonNull(lessonMaterial.getTopicId());
        createdByTeacherId = Objects.requireNonNull(lessonMaterial.getCreatedByTeacherId());
        materialType = Objects.requireNonNull(lessonMaterial.getMaterialType());
        title = Objects.requireNonNull(lessonMaterial.getTitle());
        content = lessonMaterial.getContent();
        fileAssetId = lessonMaterial.getFileAssetId();
        externalUrl = lessonMaterial.getExternalUrl();
        position = lessonMaterial.getPosition();
    }

    LessonMaterialEntity toEntity() {
        return new LessonMaterialEntity(
            id, topicId, createdByTeacherId, materialType, title, content, fileAssetId,
            externalUrl, position, version, createdAt, updatedAt
        );
    }
}
