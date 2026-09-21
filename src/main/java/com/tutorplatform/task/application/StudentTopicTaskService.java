package com.tutorplatform.task.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.LearningProgramTopicNotFoundException;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.program.application.StudentProgramNotFoundException;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfigRepository;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.programming.TaskTestCaseRepository;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudentTopicTaskService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final TopicTaskRepository topicTaskRepository;
    private final TaskRepository taskRepository;
    private final ProgrammingTaskConfigRepository programmingConfigRepository;
    private final TaskTestCaseRepository testCaseRepository;

    public StudentTopicTaskService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        TopicTaskRepository topicTaskRepository,
        TaskRepository taskRepository,
        ProgrammingTaskConfigRepository programmingConfigRepository,
        TaskTestCaseRepository testCaseRepository
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.topicTaskRepository = topicTaskRepository;
        this.taskRepository = taskRepository;
        this.programmingConfigRepository = programmingConfigRepository;
        this.testCaseRepository = testCaseRepository;
    }

    public List<StudentTopicTaskResult> listTasks(
        AuthenticatedUser principal,
        UUID studentProgramId,
        UUID topicId
    ) {
        UUID studentId = studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram = programQuery.findStudentProgram(studentProgramId)
            .filter(program -> program.belongsToStudent(studentId))
            .orElseThrow(StudentProgramNotFoundException::new);
        if (!programQuery.topicBelongsToLearningProgram(topicId, studentProgram.learningProgramId())) {
            throw new LearningProgramTopicNotFoundException();
        }

        List<TopicTaskEntity> topicTasks = topicTaskRepository.findAllByTopicIdOrderByPosition(topicId);
        Map<UUID, TaskEntity> tasksById = taskRepository.findAllById(topicTasks.stream()
                .map(TopicTaskEntity::taskId)
                .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));

        return topicTasks.stream()
            .map(topicTask -> toStudentResult(topicTask, tasksById.get(topicTask.taskId())))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    public ProgramQuery.StudentProgramContext requireTaskAccess(
        UUID studentId,
        UUID studentProgramId,
        UUID topicId,
        TaskQuery.TaskContext task
    ) {
        ProgramQuery.StudentProgramContext studentProgram = programQuery.findStudentProgram(studentProgramId)
            .filter(program -> program.belongsToStudent(studentId))
            .orElseThrow(StudentProgramNotFoundException::new);
        if (!programQuery.topicBelongsToLearningProgram(topicId, studentProgram.learningProgramId())) {
            throw new LearningProgramTopicNotFoundException();
        }
        if (!topicTaskRepository.existsByTopicIdAndTaskId(topicId, task.id())
            || !studentProgram.isAssignedBy(task.teacherId())
            || !studentProgram.subjectId().equals(task.subjectId())) {
            throw new TaskNotFoundException();
        }
        return studentProgram;
    }

    private StudentTopicTaskResult toStudentResult(TopicTaskEntity topicTask, TaskEntity task) {
        if (task == null || task.getStatus() != TaskStatus.ACTIVE
            || (task.getTaskType() != TaskType.TEXT && task.getTaskType() != TaskType.CODE)) {
            return null;
        }

        ProgrammingTaskConfig programmingConfig = null;
        List<TaskTestCase> publicTestCases = null;
        if (task.getTaskType() == TaskType.CODE) {
            programmingConfig = programmingConfigRepository.findByTaskId(task.getId()).orElse(null);
            publicTestCases = testCaseRepository.findAllByTaskId(task.getId()).stream()
                .filter(testCase -> !testCase.hidden())
                .toList();
        }

        return new StudentTopicTaskResult(
            task.getId(),
            task.getTitle(),
            task.getDescriptionMarkdown(),
            task.getTaskType(),
            task.getDifficulty(),
            topicTask.position(),
            topicTask.required(),
            programmingConfig,
            publicTestCases
        );
    }
}
