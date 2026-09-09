package com.tutorplatform.task.infrastructure.persistence.task;

import com.tutorplatform.task.application.TaskPage;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaTaskQuery implements TaskQuery {

    private final TaskDatabaseRepository databaseRepository;

    JpaTaskQuery(TaskDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public Optional<TaskContext> findTask(UUID taskId) {
        return databaseRepository.findById(taskId).map(task -> new TaskContext(
            task.getId(), task.getTeacherId(), task.getSubjectId(), task.getTitle(),
            task.getTaskType(), task.getStatus()
        ));
    }

    @Override
    public TaskPage findTeacherTextTasks(
        UUID teacherId,
        UUID subjectId,
        TaskStatus status,
        TaskDifficulty difficulty,
        int page,
        int size,
        String sortField,
        boolean ascending
    ) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result = databaseRepository.findPageByTeacher(
            teacherId,
            TaskType.TEXT,
            subjectId,
            status,
            difficulty,
            PageRequest.of(page, size, Sort.by(
                new Sort.Order(direction, sortField),
                Sort.Order.desc("id")
            ))
        );
        return new TaskPage(
            result.getContent().stream().map(TaskDatabaseModel::toEntity).toList(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<TaskContext> findTasksByIds(Set<UUID> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return databaseRepository.findAllById(taskIds).stream()
            .map(task -> new TaskContext(
                task.getId(),
                task.getTeacherId(),
                task.getSubjectId(),
                task.getTitle(),
                task.getTaskType(),
                task.getStatus()
            ))
            .toList();
    }
}
