package com.tutorplatform.program.domain;

import java.util.Optional;
import java.util.UUID;

public interface TopicRepository {
    TopicEntity saveAndFlush(TopicEntity topic);

    Optional<TopicEntity> findById(UUID topicId);

    boolean existsByModuleId(UUID moduleId);
}
