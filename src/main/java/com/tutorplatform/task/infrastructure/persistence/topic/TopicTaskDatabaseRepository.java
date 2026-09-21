package com.tutorplatform.task.infrastructure.persistence.topic;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TopicTaskDatabaseRepository extends JpaRepository<TopicTaskDatabaseModel, TopicTaskId> {
    List<TopicTaskDatabaseModel> findAllByIdTopicIdOrderByPositionAsc(UUID topicId);

    boolean existsByIdTopicIdAndIdTaskId(UUID topicId, UUID taskId);

    boolean existsByIdTopicIdAndPosition(UUID topicId, int position);
}
