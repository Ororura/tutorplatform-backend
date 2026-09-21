package com.tutorplatform.task.infrastructure.persistence.task;

import com.tutorplatform.task.application.TaskPage;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfigRepository;
import com.tutorplatform.task.domain.programming.TaskTestCaseRepository;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskQuery implements TaskQuery {

    private final TaskDatabaseRepository databaseRepository;
    private final ProgrammingTaskConfigRepository programmingConfigRepository;
    private final TaskTestCaseRepository testCaseRepository;

    JpaTaskQuery(
            TaskDatabaseRepository databaseRepository,
            ProgrammingTaskConfigRepository programmingConfigRepository,
            TaskTestCaseRepository testCaseRepository) {
        this.databaseRepository = databaseRepository;
        this.programmingConfigRepository = programmingConfigRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Override
    public Optional<TaskContext> findTask(UUID taskId) {
        return databaseRepository
                .findById(taskId)
                .map(
                        task ->
                                new TaskContext(
                                        task.getId(),
                                        task.getTeacherId(),
                                        task.getSubjectId(),
                                        task.getTitle(),
                                        task.getTaskType(),
                                        task.getStatus()));
    }

    @Override
    public Optional<CodeTaskConfiguration> findCodeTaskConfiguration(UUID taskId) {
        return databaseRepository
                .findById(taskId)
                .map(
                        task ->
                                new CodeTaskConfiguration(
                                        task.getId(),
                                        task.getTaskType(),
                                        task.getStatus(),
                                        programmingConfigRepository
                                                .findByTaskId(taskId)
                                                .orElse(null),
                                        testCaseRepository.findAllByTaskId(taskId)));
    }

    @Override
    public TaskPage findTeacherTasks(
            UUID teacherId,
            UUID subjectId,
            TaskStatus status,
            TaskDifficulty difficulty,
            int page,
            int size,
            String sortField,
            boolean ascending) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        var result =
                databaseRepository.findPageByTeacher(
                        teacherId,
                        null,
                        subjectId,
                        status,
                        difficulty,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        new Sort.Order(direction, sortField),
                                        Sort.Order.desc("id"))));
        return new TaskPage(
                result.getContent().stream().map(TaskDatabaseModel::toEntity).toList(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public List<TaskContext> findTasksByIds(Set<UUID> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return databaseRepository.findAllById(taskIds).stream()
                .map(
                        task ->
                                new TaskContext(
                                        task.getId(),
                                        task.getTeacherId(),
                                        task.getSubjectId(),
                                        task.getTitle(),
                                        task.getTaskType(),
                                        task.getStatus()))
                .toList();
    }
}
