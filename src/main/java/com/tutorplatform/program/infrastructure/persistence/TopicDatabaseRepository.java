package com.tutorplatform.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface TopicDatabaseRepository extends JpaRepository<TopicDatabaseModel, UUID> {
    boolean existsByModuleId(UUID moduleId);

    @Query("select coalesce(max(topic.position), -1) from TopicDatabaseModel topic where topic.moduleId = :moduleId")
    int findMaxPositionByModuleId(UUID moduleId);
}
