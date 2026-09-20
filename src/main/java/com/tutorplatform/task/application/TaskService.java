package com.tutorplatform.task.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.task.application.exception.*;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.user.domain.TeacherRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ProgrammingTaskConfigRepository programmingConfigRepository;
    private final TaskTestCaseRepository testCaseRepository;

    public TaskService(
        TeacherRepository teacherRepository,
        SubjectRepository subjectRepository,
        ProgramQuery programQuery,
        TaskRepository taskRepository,
        TaskQuery taskQuery,
        TopicTaskRepository topicTaskRepository,
        ProgrammingTaskConfigRepository programmingConfigRepository,
        TaskTestCaseRepository testCaseRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.subjectRepository = subjectRepository;
        this.programQuery = programQuery;
        this.taskRepository = taskRepository;
        this.taskQuery = taskQuery;
        this.topicTaskRepository = topicTaskRepository;
        this.programmingConfigRepository = programmingConfigRepository;
        this.testCaseRepository = testCaseRepository;
    }

    @Transactional
    public TaskResult createTask(AuthenticatedUser principal, CreateTaskCommand command) {
        UUID teacherId = currentTeacherId(principal);
        requireAccessibleSubject(command.subjectId(), teacherId);
        String title = validateAndNormalizeTask(
            command.title(), command.descriptionMarkdown(), command.difficulty()
        );

        TaskType taskType = command.taskType() == null ? TaskType.TEXT : command.taskType();
        if (taskType != TaskType.TEXT && taskType != TaskType.CODE) {
            throw new InvalidTaskException("taskType", "must be TEXT or CODE");
        }
        if (taskType == TaskType.TEXT && (command.programmingConfig() != null || command.testCases() != null)) {
            throw new TaskTypeMismatchException();
        }

        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(),
            teacherId,
            command.subjectId(),
            title,
            command.descriptionMarkdown(),
            taskType,
            command.difficulty(),
            TaskStatus.DRAFT
        ));
        if (taskType == TaskType.CODE) {
            ProgrammingTaskConfig config = createConfig(task.getId(), command.programmingConfig());
            List<TaskTestCase> testCases = createTestCases(task.getId(), command.testCases(), false);
            programmingConfigRepository.saveAndFlush(config);
            testCaseRepository.saveAllAndFlush(testCases);
        }
        return toResult(task);
    }

    @Transactional(readOnly = true)
    public TaskResult getTask(AuthenticatedUser principal, UUID taskId) {
        return toResult(requireOwnedTask(taskId, currentTeacherId(principal)));
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
        TaskPage result = taskQuery.findTeacherTasks(
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
        TaskEntity current = requireOwnedTask(taskId, teacherId);
        String title = validateAndNormalizeTask(
            command.title(), command.descriptionMarkdown(), command.difficulty()
        );
        if (command.status() == null) {
            throw new InvalidTaskException("status", "is required");
        }
        if (command.version() == null || command.version() < 0) {
            throw new InvalidTaskException("version", "must be greater than or equal to 0");
        }
        if (command.status() == TaskStatus.ACTIVE && current.getTaskType() == TaskType.CODE) {
            requireCodeTaskReady(current.getId());
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
    public ProgrammingTaskConfig updateProgrammingTaskConfig(
        AuthenticatedUser principal,
        UUID taskId,
        UpdateProgrammingTaskConfigCommand command
    ) {
        requireOwnedCodeTask(taskId, currentTeacherId(principal));
        ProgrammingTaskConfig current = programmingConfigRepository.findByTaskId(taskId)
            .orElseThrow(TaskNotReadyForActivationException::new);
        if (command.executionEnabled() == null) {
            throw new InvalidProgrammingTaskConfigException("executionEnabled", "is required");
        }
        if (command.timeLimitMs() == null) {
            throw new InvalidProgrammingTaskConfigException("timeLimitMs", "is required");
        }
        if (command.memoryLimitMb() == null) {
            throw new InvalidProgrammingTaskConfigException("memoryLimitMb", "is required");
        }
        try {
            return programmingConfigRepository.saveAndFlush(new ProgrammingTaskConfig(
                taskId, current.language(), command.starterCode(), command.executionEnabled(),
                command.timeLimitMs(), command.memoryLimitMb(), current.createdAt(), current.updatedAt()
            ));
        } catch (IllegalArgumentException exception) {
            throw invalidConfig(exception);
        }
    }

    @Transactional
    public List<TaskTestCase> replaceTaskTestCases(
        AuthenticatedUser principal,
        UUID taskId,
        List<TaskTestCaseInput> inputs
    ) {
        requireOwnedCodeTask(taskId, currentTeacherId(principal));
        List<TaskTestCase> replacements = createTestCases(taskId, inputs, true);
        Map<UUID, TaskTestCase> existing = new HashMap<>();
        for (TaskTestCase testCase : testCaseRepository.findAllByTaskId(taskId)) {
            existing.put(testCase.id(), testCase);
        }
        Set<UUID> ids = new HashSet<>();
        for (int index = 0; index < replacements.size(); index++) {
            TaskTestCase replacement = replacements.get(index);
            if (!ids.add(replacement.id())) {
                throw new InvalidTaskTestCaseException("items[" + index + "].id", "must be unique");
            }
            TaskTestCaseInput input = inputs.get(index);
            if (input.id() != null && !existing.containsKey(input.id())) {
                throw new InvalidTaskTestCaseException("items[" + index + "].id", "does not belong to this task");
            }
            TaskTestCase previous = existing.get(replacement.id());
            if (previous != null) {
                replacements.set(index, new TaskTestCase(
                    replacement.id(), taskId, replacement.inputText(), replacement.expectedOutput(),
                    replacement.hidden(), replacement.comparisonMode(), replacement.position(), previous.createdAt()
                ));
            }
        }
        testCaseRepository.deleteAllByTaskIdAndFlush(taskId);
        return testCaseRepository.saveAllAndFlush(replacements);
    }

    @Transactional
    public TopicTaskResult attachTaskToTopic(
        AuthenticatedUser principal,
        UUID topicId,
        UUID taskId,
        AttachTaskToTopicCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        TaskEntity task = requireOwnedTask(taskId, teacherId);
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

    @Transactional(readOnly = true)
    public List<TopicTaskDetailsResult> listTopicTasks(AuthenticatedUser principal, UUID topicId) {
        UUID teacherId = currentTeacherId(principal);
        ProgramQuery.TopicContext topic = programQuery.findTopic(topicId)
            .orElseThrow(TaskTopicNotFoundException::new);
        if (!topic.isOwnedBy(teacherId)) {
            throw new TaskTopicNotFoundException();
        }

        List<TopicTaskEntity> topicTasks = topicTaskRepository.findAllByTopicIdOrderByPosition(topicId);
        Map<UUID, TaskEntity> tasksById = taskRepository.findAllById(topicTasks.stream()
                .map(TopicTaskEntity::taskId)
                .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));

        return topicTasks.stream()
            .map(topicTask -> toDetailsResult(topicTask, tasksById.get(topicTask.taskId())))
            .toList();
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

    private TaskEntity requireOwnedTask(UUID taskId, UUID teacherId) {
        TaskEntity task = taskRepository.findOwnedById(taskId, teacherId)
            .orElseThrow(TaskNotFoundException::new);
        return task;
    }

    private TopicTaskDetailsResult toDetailsResult(TopicTaskEntity topicTask, TaskEntity task) {
        return new TopicTaskDetailsResult(
            task.getId(), task.getTitle(), task.getTaskType(), task.getDifficulty(), task.getStatus(),
            topicTask.position(), topicTask.required()
        );
    }

    private TaskEntity requireOwnedCodeTask(UUID taskId, UUID teacherId) {
        TaskEntity task = requireOwnedTask(taskId, teacherId);
        if (task.getTaskType() != TaskType.CODE) {
            throw new TaskTypeMismatchException();
        }
        return task;
    }

    private ProgrammingTaskConfig createConfig(UUID taskId, ProgrammingTaskConfigInput input) {
        if (input == null) {
            throw new InvalidProgrammingTaskConfigException("programmingConfig", "is required for CODE tasks");
        }
        if (input.language() == null) {
            throw new InvalidProgrammingTaskConfigException("programmingConfig.language", "is required");
        }
        if (input.executionEnabled() == null || input.timeLimitMs() == null || input.memoryLimitMb() == null) {
            throw new InvalidProgrammingTaskConfigException("programmingConfig", "all limits and executionEnabled are required");
        }
        try {
            return new ProgrammingTaskConfig(taskId, input.language(), input.starterCode(),
                input.executionEnabled(), input.timeLimitMs(), input.memoryLimitMb());
        } catch (IllegalArgumentException exception) {
            throw invalidConfig(exception);
        }
    }

    private InvalidProgrammingTaskConfigException invalidConfig(IllegalArgumentException exception) {
        String field = exception.getMessage().startsWith("timeLimitMs") ? "timeLimitMs" : "memoryLimitMb";
        return new InvalidProgrammingTaskConfigException(field, exception.getMessage());
    }

    private List<TaskTestCase> createTestCases(UUID taskId, List<TaskTestCaseInput> inputs, boolean preserveIds) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidTaskTestCaseException("testCases", "at least one test case is required");
        }
        Set<Integer> positions = new HashSet<>();
        List<TaskTestCase> result = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            TaskTestCaseInput input = inputs.get(index);
            String prefix = "testCases[" + index + "]";
            if (input == null) {
                throw new InvalidTaskTestCaseException(prefix, "is required");
            }
            if (input.expectedOutput() == null) {
                throw new InvalidTaskTestCaseException(prefix + ".expectedOutput", "is required");
            }
            if (input.hidden() == null || input.comparisonMode() == null || input.position() == null) {
                throw new InvalidTaskTestCaseException(prefix, "hidden, comparisonMode, and position are required");
            }
            if (!positions.add(input.position())) {
                throw new InvalidTaskTestCaseException(prefix + ".position", "must be unique within the task");
            }
            try {
                result.add(new TaskTestCase(
                    preserveIds && input.id() != null ? input.id() : UUID.randomUUID(),
                    taskId, input.inputText(), input.expectedOutput(), input.hidden(),
                    input.comparisonMode(), input.position()
                ));
            } catch (IllegalArgumentException exception) {
                throw new InvalidTaskTestCaseException(prefix + ".position", exception.getMessage());
            }
        }
        return result;
    }

    private void requireCodeTaskReady(UUID taskId) {
        ProgrammingTaskConfig config = programmingConfigRepository.findByTaskId(taskId)
            .orElseThrow(TaskNotReadyForActivationException::new);
        try {
            new ProgrammingTaskConfig(config.taskId(), config.language(), config.starterCode(),
                config.executionEnabled(), config.timeLimitMs(), config.memoryLimitMb(),
                config.createdAt(), config.updatedAt());
        } catch (IllegalArgumentException exception) {
            throw new TaskNotReadyForActivationException();
        }
        if (testCaseRepository.findAllByTaskId(taskId).isEmpty()) {
            throw new TaskNotReadyForActivationException();
        }
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
        ProgrammingTaskConfig config = task.getTaskType() == TaskType.CODE
            ? programmingConfigRepository.findByTaskId(task.getId()).orElse(null) : null;
        List<TaskTestCase> testCases = task.getTaskType() == TaskType.CODE
            ? testCaseRepository.findAllByTaskId(task.getId()) : null;
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
            task.getUpdatedAt(),
            config,
            testCases
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
