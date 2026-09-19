package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.TopicEntity;
import jakarta.persistence.OptimisticLockException;
import com.tutorplatform.program.domain.TopicRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTopicRepository implements TopicRepository {

    private final TopicDatabaseRepository databaseRepository;

    JpaTopicRepository(TopicDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public TopicEntity saveAndFlush(TopicEntity topic) {
        TopicDatabaseModel model = databaseRepository.findById(topic.id())
            .map(existing -> {
                if (!existing.hasVersion(topic.version())) {
                    throw new OptimisticLockException("Topic was modified by another transaction");
                }
                return existing;
            })
            .orElseGet(() -> new TopicDatabaseModel(topic));
        model.updateFrom(topic);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<TopicEntity> findById(UUID topicId) {
        return databaseRepository.findById(topicId).map(TopicDatabaseModel::toEntity);
    }

    @Override
    public List<TopicEntity> findByModuleId(UUID moduleId) {
        return databaseRepository.findByModuleId(moduleId).stream().map(TopicDatabaseModel::toEntity).toList();
    }

    @Override
    public boolean existsByModuleId(UUID moduleId) {
        return databaseRepository.existsByModuleId(moduleId);
    }

    @Override
    public int findMaxPositionByModuleId(UUID moduleId) {
        return databaseRepository.findMaxPositionByModuleId(moduleId);
    }

    @Override
    public void updatePosition(UUID moduleId, UUID topicId, int position) {
        if (databaseRepository.updatePosition(moduleId, topicId, position) != 1) {
            throw new IllegalStateException("Learning program topic position update failed");
        }
    }
}
