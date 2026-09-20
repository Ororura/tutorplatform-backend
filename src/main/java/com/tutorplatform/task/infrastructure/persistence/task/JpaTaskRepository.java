package com.tutorplatform.task.infrastructure.persistence.task;

import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTaskRepository implements TaskRepository {

    private final TaskDatabaseRepository databaseRepository;

    JpaTaskRepository(TaskDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public TaskEntity saveAndFlush(TaskEntity task) {
        return databaseRepository.saveAndFlush(new TaskDatabaseModel(task)).toEntity();
    }

    @Override
    public Optional<TaskEntity> findById(UUID taskId) {
        return databaseRepository.findById(taskId).map(TaskDatabaseModel::toEntity);
    }

    @Override
    public Optional<TaskEntity> findOwnedById(UUID taskId, UUID teacherId) {
        return databaseRepository.findByIdAndTeacherId(taskId, teacherId).map(TaskDatabaseModel::toEntity);
    }

    @Override
    public List<TaskEntity> findAllById(Set<UUID> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return databaseRepository.findAllById(taskIds).stream()
            .map(TaskDatabaseModel::toEntity)
            .toList();
    }

    @Override
    public List<TaskEntity> findAllByTeacherId(UUID teacherId) {
        return findAllByTeacherId(teacherId, null, null, null);
    }

    @Override
    public List<TaskEntity> findAllByTeacherId(
        UUID teacherId,
        UUID subjectId,
        TaskStatus status,
        TaskType taskType
    ) {
        return databaseRepository.findAllByTeacherId(teacherId, subjectId, status, taskType).stream()
            .map(TaskDatabaseModel::toEntity)
            .toList();
    }
}
