package com.tutorplatform.task.infrastructure.persistence.topic;

import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaTopicTaskRepository implements TopicTaskRepository {

    private final TopicTaskDatabaseRepository databaseRepository;

    JpaTopicTaskRepository(TopicTaskDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public TopicTaskEntity saveAndFlush(TopicTaskEntity topicTask) {
        return databaseRepository.saveAndFlush(new TopicTaskDatabaseModel(topicTask)).toEntity();
    }

    @Override
    public List<TopicTaskEntity> findAllByTopicIdOrderByPosition(UUID topicId) {
        return databaseRepository.findAllByIdTopicIdOrderByPositionAsc(topicId).stream()
                .map(TopicTaskDatabaseModel::toEntity)
                .toList();
    }

    @Override
    public boolean existsByTopicIdAndTaskId(UUID topicId, UUID taskId) {
        return databaseRepository.existsByIdTopicIdAndIdTaskId(topicId, taskId);
    }

    @Override
    public boolean existsByTopicIdAndPosition(UUID topicId, int position) {
        return databaseRepository.existsByIdTopicIdAndPosition(topicId, position);
    }
}
