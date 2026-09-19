package com.tutorplatform.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface TopicDatabaseRepository extends JpaRepository<TopicDatabaseModel, UUID> {
    List<TopicDatabaseModel> findByModuleId(UUID moduleId);

    boolean existsByModuleId(UUID moduleId);

    @Query("select coalesce(max(topic.position), -1) from TopicDatabaseModel topic where topic.moduleId = :moduleId")
    int findMaxPositionByModuleId(UUID moduleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update TopicDatabaseModel topic
        set topic.position = :position
        where topic.moduleId = :moduleId and topic.id = :topicId
        """)
    int updatePosition(UUID moduleId, UUID topicId, int position);
}
