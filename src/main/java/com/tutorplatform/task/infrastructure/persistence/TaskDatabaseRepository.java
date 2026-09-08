package com.tutorplatform.task.infrastructure.persistence;

import com.tutorplatform.task.domain.TaskStatus;
import com.tutorplatform.task.domain.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TaskDatabaseRepository extends JpaRepository<TaskDatabaseModel, UUID> {

    Optional<TaskDatabaseModel> findByIdAndTeacherId(UUID taskId, UUID teacherId);

    @Query("""
        select task
        from TaskDatabaseModel task
        where task.teacherId = :teacherId
          and (:subjectId is null or task.subjectId = :subjectId)
          and (:status is null or task.status = :status)
          and (:taskType is null or task.taskType = :taskType)
        order by task.createdAt desc
        """)
    List<TaskDatabaseModel> findAllByTeacherId(
            @Param("teacherId") UUID teacherId,
            @Param("subjectId") UUID subjectId,
            @Param("status") TaskStatus status,
            @Param("taskType") TaskType taskType
    );
}
