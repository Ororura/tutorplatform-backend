package com.tutorplatform.content.infrastructure.persistence;

import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaLessonMaterialRepository implements LessonMaterialRepository {

    private final LessonMaterialDatabaseRepository databaseRepository;

    JpaLessonMaterialRepository(LessonMaterialDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public LessonMaterialEntity save(LessonMaterialEntity lessonMaterial) {
        return databaseRepository.save(new LessonMaterialDatabaseModel(lessonMaterial)).toEntity();
    }

    @Override
    public LessonMaterialEntity saveAndFlush(LessonMaterialEntity lessonMaterial) {
        return databaseRepository.saveAndFlush(new LessonMaterialDatabaseModel(lessonMaterial)).toEntity();
    }

    @Override
    public Optional<LessonMaterialEntity> findById(UUID lessonMaterialId) {
        return databaseRepository.findById(lessonMaterialId).map(LessonMaterialDatabaseModel::toEntity);
    }

    @Override
    public Optional<LessonMaterialEntity> findByIdAndTopicId(UUID lessonMaterialId, UUID topicId) {
        return databaseRepository.findByIdAndTopicId(lessonMaterialId, topicId)
            .map(LessonMaterialDatabaseModel::toEntity);
    }

    @Override
    public List<LessonMaterialEntity> findAllByTopicIdOrderByPosition(UUID topicId) {
        return databaseRepository.findAllByTopicIdOrderByPositionAsc(topicId).stream()
            .map(LessonMaterialDatabaseModel::toEntity)
            .toList();
    }

    @Override
    public boolean existsByIdAndTopicId(UUID lessonMaterialId, UUID topicId) {
        return databaseRepository.existsByIdAndTopicId(lessonMaterialId, topicId);
    }
}
