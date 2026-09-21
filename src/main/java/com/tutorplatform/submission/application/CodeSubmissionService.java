package com.tutorplatform.submission.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.submission.application.exception.*;
import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.task.application.StudentTopicTaskService;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CodeSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(CodeSubmissionService.class);
    private static final int MAX_EXCERPT_BYTES = 16_384;

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final TaskQuery taskQuery;
    private final SubmissionHomeworkContextQuery homeworkContextQuery;
    private final ProgramQuery programQuery;
    private final StudentTopicTaskService studentTopicTaskService;
    private final CodeSubmissionTransactions transactions;
    private final ExecutionPort executionPort;

    public CodeSubmissionService(
            StudentOwnershipQuery studentOwnershipQuery,
            TaskQuery taskQuery,
            SubmissionHomeworkContextQuery homeworkContextQuery,
            ProgramQuery programQuery,
            StudentTopicTaskService studentTopicTaskService,
            CodeSubmissionTransactions transactions,
            ExecutionPort executionPort) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.taskQuery = taskQuery;
        this.homeworkContextQuery = homeworkContextQuery;
        this.programQuery = programQuery;
        this.studentTopicTaskService = studentTopicTaskService;
        this.transactions = transactions;
        this.executionPort = executionPort;
    }

    /** Orchestrates execution deliberately without a surrounding database transaction. */
    public SubmissionResult submit(
            AuthenticatedUser principal,
            UUID taskId,
            UUID homeworkItemId,
            UUID studentProgramId,
            UUID topicId,
            String sourceCode) {
        validateRequest(homeworkItemId, studentProgramId, topicId, sourceCode);
        UUID studentId =
                studentOwnershipQuery
                        .findStudentIdByUserId(principal.id())
                        .orElseThrow(StudentNotFoundException::new);
        TaskQuery.TaskContext task =
                taskQuery.findTask(taskId).orElseThrow(TaskNotFoundException::new);
        SubmissionContext submissionContext =
                resolveSubmissionContext(
                        studentId, task, homeworkItemId, studentProgramId, topicId);
        TaskQuery.CodeTaskConfiguration codeTask =
                taskQuery.findCodeTaskConfiguration(taskId).orElseThrow(TaskNotFoundException::new);
        requireExecutable(codeTask);

        int configuredTestCount = codeTask.testCases().size();
        SubmissionResult pending =
                transactions.createPending(
                        studentId,
                        submissionContext.studentProgramId(),
                        taskId,
                        homeworkItemId,
                        sourceCode,
                        configuredTestCount);

        UUID executionId = UUID.randomUUID();
        ExecutionResult executionResult;
        try {
            executionResult =
                    executionPort.execute(toExecutionRequest(executionId, sourceCode, codeTask));
            if (!validResult(executionResult, executionId, configuredTestCount)) {
                executionResult = ExecutionResult.systemError(executionId, configuredTestCount);
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "Submitted code execution failed at infrastructure boundary: submissionId={}, failureType={}",
                    pending.id(),
                    exception.getClass().getSimpleName());
            executionResult = ExecutionResult.systemError(executionId, configuredTestCount);
        }

        boolean systemError = executionResult.status() == ExecutionStatus.SYSTEM_ERROR;
        boolean hasHiddenTests = codeTask.testCases().stream().anyMatch(TaskTestCase::hidden);
        return transactions.finish(
                pending.id(),
                CodeExecutionStatus.valueOf(executionResult.status().name()),
                systemError ? 0 : executionResult.passedTests(),
                configuredTestCount,
                systemError ? 0 : executionResult.executionTimeMs(),
                systemError || hasHiddenTests ? null : bounded(executionResult.stdoutExcerpt()),
                systemError || hasHiddenTests ? null : bounded(executionResult.stderrExcerpt()));
    }

    private void validateRequest(
            UUID homeworkItemId, UUID studentProgramId, UUID topicId, String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new InvalidSubmissionException("sourceCode", "must not be blank");
        }
        boolean homeworkContext = homeworkItemId != null;
        boolean topicContext = studentProgramId != null && topicId != null;
        if (homeworkContext == topicContext
                || (homeworkContext && (studentProgramId != null || topicId != null))
                || (!homeworkContext && !topicContext)) {
            throw new InvalidSubmissionException(
                    "context",
                    "must contain either homeworkItemId or studentProgramId and topicId");
        }
    }

    private SubmissionContext resolveSubmissionContext(
            UUID studentId,
            TaskQuery.TaskContext task,
            UUID homeworkItemId,
            UUID studentProgramId,
            UUID topicId) {
        if (homeworkItemId != null) {
            SubmissionHomeworkContextQuery.HomeworkSubmissionContext homework =
                    requireOwnedHomeworkContext(studentId, task, homeworkItemId);
            return new SubmissionContext(homework.studentProgramId());
        }
        ProgramQuery.StudentProgramContext program =
                studentTopicTaskService.requireTaskAccess(
                        studentId, studentProgramId, topicId, task);
        return new SubmissionContext(program.id());
    }

    private SubmissionHomeworkContextQuery.HomeworkSubmissionContext requireOwnedHomeworkContext(
            UUID studentId, TaskQuery.TaskContext task, UUID homeworkItemId) {
        var homework =
                homeworkContextQuery
                        .findSubmissionContext(homeworkItemId)
                        .orElseThrow(HomeworkItemNotFoundException::new);
        ProgramQuery.StudentProgramContext program =
                programQuery
                        .findStudentProgram(homework.studentProgramId())
                        .orElseThrow(HomeworkItemNotFoundException::new);
        if (!program.belongsToStudent(studentId)) {
            throw new HomeworkItemNotFoundException();
        }
        if (!homework.taskId().equals(task.id())) {
            throw new SubmissionContextInvalidException();
        }
        if (!program.isAssignedBy(homework.assignedByTeacherId())
                || !task.teacherId().equals(homework.assignedByTeacherId())
                || !task.subjectId().equals(program.subjectId())) {
            throw new SubmissionContextInvalidException();
        }
        if (homework.cancelled() || homework.completed()) {
            throw new HomeworkNotSubmittableException();
        }
        return homework;
    }

    private void requireExecutable(TaskQuery.CodeTaskConfiguration task) {
        if (task.type() != TaskType.CODE
                || task.status() != TaskStatus.ACTIVE
                || task.programmingConfig() == null
                || task.testCases().isEmpty()) {
            throw new InvalidSubmissionException(
                    "taskId", "must reference an active executable CODE task");
        }
        if (!task.programmingConfig().executionEnabled()) {
            throw new InvalidSubmissionException("taskId", "code execution is disabled");
        }
    }

    private ExecutionRequest toExecutionRequest(
            UUID executionId, String sourceCode, TaskQuery.CodeTaskConfiguration task) {
        var config = task.programmingConfig();
        return new ExecutionRequest(
                executionId,
                ExecutionLanguage.valueOf(config.language().name()),
                sourceCode,
                config.timeLimitMs(),
                config.memoryLimitMb(),
                task.testCases().stream()
                        .map(
                                test ->
                                        new ExecutionTestCase(
                                                test.id(),
                                                test.inputText(),
                                                test.expectedOutput(),
                                                ExecutionComparisonMode.valueOf(
                                                        test.comparisonMode().name())))
                        .toList());
    }

    private boolean validResult(ExecutionResult result, UUID executionId, int configuredTestCount) {
        return result != null
                && executionId.equals(result.executionId())
                && result.totalTests() == configuredTestCount
                && result.passedTests() <= configuredTestCount;
    }

    private String bounded(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_EXCERPT_BYTES) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        int usedBytes = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int bytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + bytes > MAX_EXCERPT_BYTES) {
                break;
            }
            result.append(character);
            usedBytes += bytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private record SubmissionContext(UUID studentProgramId) {}
}
