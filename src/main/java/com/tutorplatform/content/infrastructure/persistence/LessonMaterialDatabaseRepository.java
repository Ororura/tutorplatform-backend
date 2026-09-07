package com.tutorplatform.content.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LessonMaterialDatabaseRepository extends JpaRepository<LessonMaterialDatabaseModel, UUID> {
    Optional<LessonMaterialDatabaseModel> findByIdAndTopicId(UUID lessonMaterialId, UUID topicId);
    List<LessonMaterialDatabaseModel> findAllByTopicIdOrderByPositionAsc(UUID topicId);
    boolean existsByIdAndTopicId(UUID lessonMaterialId, UUID topicId);
}
