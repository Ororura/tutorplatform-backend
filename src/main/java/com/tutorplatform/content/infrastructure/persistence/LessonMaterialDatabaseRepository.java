package com.tutorplatform.content.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LessonMaterialDatabaseRepository extends JpaRepository<LessonMaterialDatabaseModel, UUID> {
    Optional<LessonMaterialDatabaseModel> findByIdAndTopicId(UUID lessonMaterialId, UUID topicId);

    List<LessonMaterialDatabaseModel> findAllByTopicIdOrderByPositionAsc(UUID topicId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update LessonMaterialDatabaseModel material
        set material.position = :position
        where material.topicId = :topicId and material.id = :lessonMaterialId
        """)
    int updatePosition(UUID topicId, UUID lessonMaterialId, int position);

    boolean existsByIdAndTopicId(UUID lessonMaterialId, UUID topicId);
}
