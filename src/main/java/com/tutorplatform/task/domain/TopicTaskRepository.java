package com.tutorplatform.task.domain;

import java.util.List;
import java.util.UUID;

public interface TopicTaskRepository {
    TopicTaskEntity saveAndFlush(TopicTaskEntity topicTask);
    List<TopicTaskEntity> findAllByTopicIdOrderByPosition(UUID topicId);
}
