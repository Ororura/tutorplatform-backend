package com.tutorplatform.content.application;

import com.tutorplatform.content.domain.LessonMaterialType;

import java.time.Instant;
import java.util.UUID;

public record LessonMaterialResult(
    UUID id,
    UUID topicId,
    UUID createdByTeacherId,
    LessonMaterialType materialType,
    String title,
    String content,
    UUID fileAssetId,
    String externalUrl,
    int position,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
}
