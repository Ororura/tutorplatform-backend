package com.tutorplatform.program.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    TopicEntity saveAndFlush(TopicEntity topic);

    Optional<TopicEntity> findById(UUID topicId);

    List<TopicEntity> findByModuleId(UUID moduleId);

    List<TopicEntity> findByLearningProgramIdAndIdIn(UUID programId, List<UUID> topicIds);

    boolean existsByModuleId(UUID moduleId);

    int findMaxPositionByModuleId(UUID moduleId);

    void updatePosition(UUID moduleId, UUID topicId, int position);
}
