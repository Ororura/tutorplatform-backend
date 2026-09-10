package com.tutorplatform.homework.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.application.exception.*;
import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.StudentOwnershipQuery;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class HomeworkService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "assignedAt", "dueAt", "createdAt", "title"
    );

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final TaskQuery taskQuery;
    private final HomeworkRepository homeworkRepository;
    private final HomeworkQuery homeworkQuery;

    public HomeworkService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        TaskQuery taskQuery,
        HomeworkRepository homeworkRepository,
        HomeworkQuery homeworkQuery
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.taskQuery = taskQuery;
        this.homeworkRepository = homeworkRepository;
        this.homeworkQuery = homeworkQuery;
    }

    @Transactional
    public HomeworkResult createHomework(AuthenticatedUser principal, CreateHomeworkCommand command) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, command.studentId());
        ProgramQuery.StudentProgramContext studentProgram = requireStudentProgram(
            command.studentProgramId(), command.studentId(), teacherId
        );
        String title = validateTitle(command.title());
        ValidatedItems validatedItems = validateItems(
            command.items(), teacherId, studentProgram.subjectId()
        );
        UUID homeworkId = UUID.randomUUID();
        HomeworkEntity homework = new HomeworkEntity(
            homeworkId,
            studentProgram.id(),
            teacherId,
            title,
            command.description(),
            Instant.now(),
            command.dueAt(),
            HomeworkStatus.ASSIGNED,
            null,
            createItems(homeworkId, command.items(), Map.of())
        );
        try {
            return toResult(homeworkRepository.saveAndFlush(homework), validatedItems.tasksById());
        } catch (DataIntegrityViolationException exception) {
            throw new HomeworkItemPositionConflictException(exception);
        }
    }

    @Transactional(readOnly = true)
    public HomeworkResult getHomework(
        AuthenticatedUser principal,
        UUID studentId,
        UUID homeworkId
    ) {
        UUID teacherId = currentTeacherId(principal);
        HomeworkEntity homework = requireOwnedHomework(teacherId, studentId, homeworkId);
        return toResult(homework, taskMap(homework.getItems()));
    }

    @Transactional(readOnly = true)
    public HomeworkPageResult listHomeworks(
        AuthenticatedUser principal,
        UUID studentId,
        UUID studentProgramId,
        HomeworkStatus status,
        int page,
        int size,
        String sort
    ) {
        SortParameters sortParameters = validateListParameters(page, size, sort);
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        if (studentProgramId != null) {
            requireStudentProgram(studentProgramId, studentId, teacherId);
        }
        HomeworkPage result = homeworkQuery.findPageByTeacherAndStudent(
            teacherId, studentId, studentProgramId, status, page, size,
            sortParameters.field(), sortParameters.ascending()
        );
        Instant now = Instant.now();
        return new HomeworkPageResult(
            result.items().stream().map(item -> new HomeworkSummaryResult(
                item.id(), item.studentProgramId(), item.title(), item.status(),
                item.assignedAt(), item.dueAt(), isOverdue(
                item.dueAt(), item.status(), item.completedAt(), now
            ), item.createdAt()
            )).toList(),
            page,
            size,
            result.totalElements(),
            result.totalPages()
        );
    }

    @Transactional
    public HomeworkResult updateHomework(
        AuthenticatedUser principal,
        UUID studentId,
        UUID homeworkId,
        UpdateHomeworkCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        HomeworkEntity current = requireOwnedHomework(teacherId, studentId, homeworkId);
        if (current.getStatus() != HomeworkStatus.ASSIGNED) {
            throw new InvalidHomeworkException("status", "only assigned homework can be updated");
        }
        if (command.version() == null || command.version() < 0) {
            throw new InvalidHomeworkException("version", "must be greater than or equal to 0");
        }
        ProgramQuery.StudentProgramContext studentProgram = requireStudentProgram(
            current.getStudentProgramId(), studentId, teacherId
        );
        String title = validateTitle(command.title());
        ValidatedItems validatedItems = validateItems(
            command.items(), teacherId, studentProgram.subjectId()
        );
        Map<Integer, UUID> existingIdsByPosition = new HashMap<>();
        for (HomeworkItemEntity item : current.getItems()) {
            existingIdsByPosition.put(item.position(), item.id());
        }
        HomeworkEntity updated = new HomeworkEntity(
            current.getId(),
            current.getStudentProgramId(),
            current.getAssignedByTeacherId(),
            title,
            command.description(),
            current.getAssignedAt(),
            command.dueAt(),
            current.getStatus(),
            current.getCompletedAt(),
            createItems(current.getId(), command.items(), existingIdsByPosition),
            command.version(),
            current.getCreatedAt(),
            current.getUpdatedAt()
        );
        try {
            return toResult(homeworkRepository.saveAndFlush(updated), validatedItems.tasksById());
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new HomeworkVersionConflictException(exception);
        } catch (DataIntegrityViolationException exception) {
            throw new HomeworkItemPositionConflictException(exception);
        }
    }

    @Transactional
    public HomeworkResult cancelHomework(
        AuthenticatedUser principal,
        UUID studentId,
        UUID homeworkId
    ) {
        UUID teacherId = currentTeacherId(principal);
        HomeworkEntity current = requireOwnedHomework(teacherId, studentId, homeworkId);
        if (current.getStatus() == HomeworkStatus.CANCELLED) {
            return toResult(current, taskMap(current.getItems()));
        }
        if (current.getStatus() != HomeworkStatus.ASSIGNED) {
            throw new InvalidHomeworkException("status", "only assigned homework can be cancelled");
        }
        current.update(
            current.getTitle(), current.getDescription(), current.getDueAt(),
            HomeworkStatus.CANCELLED, null
        );
        try {
            return toResult(homeworkRepository.saveAndFlush(current), taskMap(current.getItems()));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new HomeworkVersionConflictException(exception);
        }
    }

    private UUID currentTeacherId(AuthenticatedUser principal) {
        return studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
    }

    private void requireOwnedStudent(UUID teacherId, UUID studentId) {
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
    }

    private ProgramQuery.StudentProgramContext requireStudentProgram(
        UUID studentProgramId,
        UUID studentId,
        UUID teacherId
    ) {
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(studentProgramId)
            .orElseThrow(HomeworkStudentProgramNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId) || !studentProgram.isAssignedBy(teacherId)) {
            throw new HomeworkStudentProgramNotFoundException();
        }
        return studentProgram;
    }

    private HomeworkEntity requireOwnedHomework(UUID teacherId, UUID studentId, UUID homeworkId) {
        requireOwnedStudent(teacherId, studentId);
        HomeworkEntity homework = homeworkRepository.findByIdWithItems(homeworkId)
            .orElseThrow(HomeworkNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(homework.getStudentProgramId())
            .orElseThrow(HomeworkNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId)
            || !studentProgram.isAssignedBy(teacherId)
            || !homework.getAssignedByTeacherId().equals(teacherId)) {
            throw new HomeworkNotFoundException();
        }
        return homework;
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidHomeworkException("title", "is required");
        }
        String normalized = title.trim();
        if (normalized.length() > 220) {
            throw new InvalidHomeworkException("title", "must contain at most 220 characters");
        }
        return normalized;
    }

    private ValidatedItems validateItems(List<HomeworkItemInput> items, UUID teacherId, UUID subjectId) {
        if (items == null || items.isEmpty()) {
            throw new InvalidHomeworkException("items", "must not be empty");
        }
        Set<UUID> taskIds = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (HomeworkItemInput item : items) {
            if (item == null || item.taskId() == null) {
                throw new InvalidHomeworkException("items", "taskId is required");
            }
            if (!taskIds.add(item.taskId())) {
                throw new InvalidHomeworkException("items", "duplicate taskId is not allowed");
            }
            if (item.position() < 0) {
                throw new InvalidHomeworkException("items.position", "must be greater than or equal to 0");
            }
            if (!positions.add(item.position())) {
                throw new HomeworkItemPositionConflictException();
            }
        }
        Map<UUID, TaskQuery.TaskContext> tasksById = new HashMap<>();
        for (TaskQuery.TaskContext task : taskQuery.findTasksByIds(taskIds)) {
            tasksById.put(task.id(), task);
        }
        for (UUID taskId : taskIds) {
            TaskQuery.TaskContext task = tasksById.get(taskId);
            if (task == null || !task.isOwnedBy(teacherId)) {
                throw new TaskNotFoundException();
            }
            if ((task.type() != TaskType.TEXT && task.type() != TaskType.CODE)
                || task.status() != TaskStatus.ACTIVE) {
                throw new HomeworkTaskNotAssignableException();
            }
            if (!task.subjectId().equals(subjectId)) {
                throw new HomeworkTaskSubjectMismatchException();
            }
        }
        return new ValidatedItems(tasksById);
    }

    private List<HomeworkItemEntity> createItems(
        UUID homeworkId,
        List<HomeworkItemInput> inputs,
        Map<Integer, UUID> existingIdsByPosition
    ) {
        return inputs.stream()
            .map(input -> new HomeworkItemEntity(
                existingIdsByPosition.getOrDefault(input.position(), UUID.randomUUID()),
                homeworkId,
                input.taskId(),
                input.position(),
                input.required()
            ))
            .sorted(Comparator.comparingInt(HomeworkItemEntity::position))
            .toList();
    }

    private Map<UUID, TaskQuery.TaskContext> taskMap(List<HomeworkItemEntity> items) {
        Set<UUID> taskIds = new HashSet<>();
        items.forEach(item -> taskIds.add(item.taskId()));
        Map<UUID, TaskQuery.TaskContext> result = new HashMap<>();
        taskQuery.findTasksByIds(taskIds).forEach(task -> result.put(task.id(), task));
        return result;
    }

    private HomeworkResult toResult(
        HomeworkEntity homework,
        Map<UUID, TaskQuery.TaskContext> tasksById
    ) {
        Instant now = Instant.now();
        List<HomeworkItemResult> items = new ArrayList<>();
        homework.getItems().stream()
            .sorted(Comparator.comparingInt(HomeworkItemEntity::position))
            .forEach(item -> {
                TaskQuery.TaskContext task = tasksById.get(item.taskId());
                items.add(new HomeworkItemResult(
                    item.id(), item.taskId(), task == null ? null : task.title(),
                    item.position(), item.required()
                ));
            });
        return new HomeworkResult(
            homework.getId(), homework.getStudentProgramId(), homework.getTitle(),
            homework.getDescription(), homework.getStatus(), homework.getAssignedAt(),
            homework.getDueAt(), isOverdue(
            homework.getDueAt(), homework.getStatus(), homework.getCompletedAt(), now
        ), homework.getCompletedAt(), List.copyOf(items), homework.getVersion(),
            homework.getCreatedAt(), homework.getUpdatedAt()
        );
    }

    private boolean isOverdue(
        Instant dueAt,
        HomeworkStatus status,
        Instant completedAt,
        Instant now
    ) {
        return dueAt != null
            && dueAt.isBefore(now)
            && status == HomeworkStatus.ASSIGNED
            && completedAt == null;
    }

    private SortParameters validateListParameters(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidHomeworkException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidHomeworkException("size", "must be between 1 and 100");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
            throw new InvalidHomeworkException(
                "sort", "must use assignedAt, dueAt, createdAt, or title"
            );
        }
        if (!parts[1].equals("asc") && !parts[1].equals("desc")) {
            throw new InvalidHomeworkException("sort", "direction must be asc or desc");
        }
        return new SortParameters(parts[0], parts[1].equals("asc"));
    }

    private record ValidatedItems(Map<UUID, TaskQuery.TaskContext> tasksById) {
    }

    private record SortParameters(String field, boolean ascending) {
    }
}
