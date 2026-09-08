package com.tutorplatform.task.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TopicTaskDatabaseRepository extends JpaRepository<TopicTaskDatabaseModel, TopicTaskId> {
    List<TopicTaskDatabaseModel> findAllByIdTopicIdOrderByPositionAsc(UUID topicId);
}
