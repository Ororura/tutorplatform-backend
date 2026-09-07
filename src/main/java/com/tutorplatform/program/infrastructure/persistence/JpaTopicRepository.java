package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import org.springframework.stereotype.Repository;

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
        return databaseRepository.saveAndFlush(new TopicDatabaseModel(topic)).toEntity();
    }

    @Override
    public Optional<TopicEntity> findById(UUID topicId) {
        return databaseRepository.findById(topicId).map(TopicDatabaseModel::toEntity);
    }
}
