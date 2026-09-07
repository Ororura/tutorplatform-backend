package com.tutorplatform.content.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonMaterialRepository {
    LessonMaterialEntity save(LessonMaterialEntity lessonMaterial);
    LessonMaterialEntity saveAndFlush(LessonMaterialEntity lessonMaterial);
    Optional<LessonMaterialEntity> findById(UUID lessonMaterialId);
    Optional<LessonMaterialEntity> findByIdAndTopicId(UUID lessonMaterialId, UUID topicId);
    List<LessonMaterialEntity> findAllByTopicIdOrderByPosition(UUID topicId);
    boolean existsByIdAndTopicId(UUID lessonMaterialId, UUID topicId);
}
