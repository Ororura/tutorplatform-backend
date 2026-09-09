package com.tutorplatform.task.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.task.application.exception.InvalidTaskException;
import com.tutorplatform.task.application.exception.InvalidTaskListParameterException;
import com.tutorplatform.task.application.exception.TaskAlreadyAttachedException;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.application.exception.TaskSubjectMismatchException;
import com.tutorplatform.task.application.exception.TaskSubjectNotFoundException;
import com.tutorplatform.task.application.exception.TaskTopicNotFoundException;
import com.tutorplatform.task.application.exception.TaskTopicPositionConflictException;
import com.tutorplatform.task.application.exception.TaskVersionConflictException;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.user.domain.TeacherRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "title", "difficulty"
    );

    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ProgramQuery programQuery;
    private final TaskRepository taskRepository;
    private final TaskQuery taskQuery;
    private final TopicTaskRepository topicTaskRepository;

    public TaskService(
            TeacherRepository teacherRepository,
            SubjectRepository subjectRepository,
            ProgramQuery programQuery,
            TaskRepository taskRepository,
            TaskQuery taskQuery,
            TopicTaskRepository topicTaskRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.programQuery = programQuery;
        this.taskRepository = taskRepository;
        this.taskQuery = taskQuery;
        this.topicTaskRepository = topicTaskRepository;
    }

    @Transactional
    public TaskResult createTask(AuthenticatedUser principal, CreateTaskCommand command) {
        UUID teacherId = currentTeacherId(principal);
        requireAccessibleSubject(command.subjectId(), teacherId);
        String title = validateAndNormalizeTask(
                command.title(), command.descriptionMarkdown(), command.difficulty()
        );

        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
                UUID.randomUUID(),
                teacherId,
                command.subjectId(),
                title,
                command.descriptionMarkdown(),
                TaskType.TEXT,
                command.difficulty(),
                TaskStatus.DRAFT
        ));
        return toResult(task);
    }

    @Transactional(readOnly = true)
    public TaskResult getTask(AuthenticatedUser principal, UUID taskId) {
        return toResult(requireOwnedTextTask(taskId, currentTeacherId(principal)));
    }

    @Transactional(readOnly = true)
    public TaskPageResult listTasks(
            AuthenticatedUser principal,
            UUID subjectId,
            TaskStatus status,
            TaskDifficulty difficulty,
            int page,
            int size,
            String sort
    ) {
        SortParameters sortParameters = validateListParameters(page, size, sort);
        TaskPage result = taskQuery.findTeacherTextTasks(
                currentTeacherId(principal),
                subjectId,
                status,
                difficulty,
                page,
                size,
                sortParameters.field(),
                sortParameters.ascending()
        );
        return new TaskPageResult(
                result.items().stream().map(this::toResult).toList(),
                page,
                size,
                result.totalElements(),
                result.totalPages()
        );
    }

    @Transactional
    public TaskResult updateTask(
            AuthenticatedUser principal,
            UUID taskId,
            UpdateTaskCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        TaskEntity current = requireOwnedTextTask(taskId, teacherId);
        String title = validateAndNormalizeTask(
                command.title(), command.descriptionMarkdown(), command.difficulty()
        );
        if (command.status() == null) {
            throw new InvalidTaskException("status", "is required");
        }
        if (command.version() == null || command.version() < 0) {
            throw new InvalidTaskException("version", "must be greater than or equal to 0");
        }

        TaskEntity updated = new TaskEntity(
                current.getId(),
                current.getTeacherId(),
                current.getSubjectId(),
                title,
                command.descriptionMarkdown(),
                current.getTaskType(),
                command.difficulty(),
                command.status(),
                command.version(),
                current.getCreatedAt(),
                current.getUpdatedAt()
        );
        try {
            return toResult(taskRepository.saveAndFlush(updated));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new TaskVersionConflictException(exception);
        }
    }

    @Transactional
    public TopicTaskResult attachTaskToTopic(
            AuthenticatedUser principal,
            UUID topicId,
            UUID taskId,
            AttachTaskToTopicCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        TaskEntity task = requireOwnedTextTask(taskId, teacherId);
        ProgramQuery.TopicContext topic = programQuery.findTopic(topicId)
                .orElseThrow(TaskTopicNotFoundException::new);
        if (!topic.isOwnedBy(teacherId)) {
            throw new TaskTopicNotFoundException();
        }
        if (!task.getSubjectId().equals(topic.subjectId())) {
            throw new TaskSubjectMismatchException();
        }
        if (command.position() < 0) {
            throw new InvalidTaskException("position", "must be greater than or equal to 0");
        }
        if (topicTaskRepository.existsByTopicIdAndTaskId(topicId, taskId)) {
            throw new TaskAlreadyAttachedException();
        }
        if (topicTaskRepository.existsByTopicIdAndPosition(topicId, command.position())) {
            throw new TaskTopicPositionConflictException();
        }

        try {
            return toResult(topicTaskRepository.saveAndFlush(new TopicTaskEntity(
                    topicId, taskId, command.position(), command.required()
            )));
        } catch (DataIntegrityViolationException exception) {
            throw new TaskTopicPositionConflictException(exception);
        }
    }

    private UUID currentTeacherId(AuthenticatedUser principal) {
        return teacherRepository.findByUserId(principal.id()).orElseThrow().id();
    }

    private SubjectEntity requireAccessibleSubject(UUID subjectId, UUID teacherId) {
        if (subjectId == null) {
            throw new TaskSubjectNotFoundException();
        }
        SubjectEntity subject = subjectRepository.findById(subjectId)
                .orElseThrow(TaskSubjectNotFoundException::new);
        if (subject.ownerTeacherId() != null && !subject.ownerTeacherId().equals(teacherId)) {
            throw new TaskSubjectNotFoundException();
        }
        return subject;
    }

    private TaskEntity requireOwnedTextTask(UUID taskId, UUID teacherId) {
        TaskEntity task = taskRepository.findOwnedById(taskId, teacherId)
                .orElseThrow(TaskNotFoundException::new);
        if (task.getTaskType() != TaskType.TEXT) {
            throw new TaskNotFoundException();
        }
        return task;
    }

    private String validateAndNormalizeTask(
            String title,
            String descriptionMarkdown,
            TaskDifficulty difficulty
    ) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskException("title", "is required");
        }
        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > 220) {
            throw new InvalidTaskException("title", "must contain at most 220 characters");
        }
        if (descriptionMarkdown == null) {
            throw new InvalidTaskException("descriptionMarkdown", "is required");
        }
        if (difficulty == null) {
            throw new InvalidTaskException("difficulty", "is required");
        }
        return normalizedTitle;
    }

    private SortParameters validateListParameters(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidTaskListParameterException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidTaskListParameterException("size", "must be between 1 and 100");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !ALLOWED_SORT_FIELDS.contains(sortParts[0])) {
            throw new InvalidTaskListParameterException(
                    "sort", "must use createdAt, updatedAt, title, or difficulty"
            );
        }
        if (!sortParts[1].equals("asc") && !sortParts[1].equals("desc")) {
            throw new InvalidTaskListParameterException("sort", "direction must be asc or desc");
        }
        return new SortParameters(sortParts[0], sortParts[1].equals("asc"));
    }

    private TaskResult toResult(TaskEntity task) {
        return new TaskResult(
                task.getId(),
                task.getSubjectId(),
                task.getTitle(),
                task.getDescriptionMarkdown(),
                task.getTaskType(),
                task.getDifficulty(),
                task.getStatus(),
                task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private TopicTaskResult toResult(TopicTaskEntity topicTask) {
        return new TopicTaskResult(
                topicTask.topicId(),
                topicTask.taskId(),
                topicTask.position(),
                topicTask.required(),
                topicTask.createdAt()
        );
    }

    private record SortParameters(String field, boolean ascending) {
    }
}
