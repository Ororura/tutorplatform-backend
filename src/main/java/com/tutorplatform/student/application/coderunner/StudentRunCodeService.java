package com.tutorplatform.student.application.coderunner;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.homework.application.HomeworkQuery;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.student.application.exception.RunCodeException;
import com.tutorplatform.student.application.exception.RunCodeException.Reason;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StudentRunCodeService {

    private static final Logger log = LoggerFactory.getLogger(StudentRunCodeService.class);
    private static final int MAX_RESPONSE_OUTPUT_BYTES = 16_384;

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final HomeworkQuery homeworkQuery;
    private final TaskQuery taskQuery;
    private final ExecutionPort executionPort;

    public StudentRunCodeService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        HomeworkQuery homeworkQuery,
        TaskQuery taskQuery,
        ExecutionPort executionPort
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.homeworkQuery = homeworkQuery;
        this.taskQuery = taskQuery;
        this.executionPort = executionPort;
    }

    public RunCodeResult run(
        AuthenticatedUser principal,
        UUID taskId,
        UUID homeworkItemId,
        String sourceCode
    ) {
        if (taskId == null) {
            throw new RunCodeException(Reason.TASK_NOT_FOUND);
        }
        if (homeworkItemId == null || sourceCode == null || sourceCode.isBlank()) {
            throw new RunCodeException(Reason.EXECUTION_CONTEXT_INVALID);
        }

        UUID studentId = studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
        TaskQuery.TaskContext taskContext = taskQuery.findTask(taskId)
            .orElseThrow(() -> new RunCodeException(Reason.TASK_NOT_FOUND));
        requireOwnedHomeworkContext(studentId, taskContext, homeworkItemId);
        TaskQuery.CodeTaskConfiguration task = taskQuery.findCodeTaskConfiguration(taskId)
            .orElseThrow(() -> new RunCodeException(Reason.TASK_NOT_FOUND));
        requireExecutableTask(task);

        var config = task.programmingConfig();
        UUID executionId = UUID.randomUUID();
        ExecutionRequest executionRequest = new ExecutionRequest(
            executionId,
            ExecutionLanguage.valueOf(config.language().name()),
            sourceCode,
            config.timeLimitMs(),
            config.memoryLimitMb(),
            task.testCases().stream().map(testCase -> new ExecutionTestCase(
                testCase.id(),
                testCase.inputText(),
                testCase.expectedOutput(),
                ExecutionComparisonMode.valueOf(testCase.comparisonMode().name())
            )).toList()
        );

        ExecutionResult executionResult;
        try {
            executionResult = executionPort.execute(executionRequest);
        } catch (RuntimeException exception) {
            log.warn(
                "Code execution failed at the infrastructure boundary: executionId={}, failureType={}",
                executionId, exception.getClass().getSimpleName()
            );
            executionResult = ExecutionResult.systemError(executionId, task.testCases().size());
        }
        if (executionResult == null || !executionId.equals(executionResult.executionId())) {
            executionResult = ExecutionResult.systemError(executionId, task.testCases().size());
        }
        return toStudentResult(executionResult, task.testCases());
    }

    private void requireExecutableTask(TaskQuery.CodeTaskConfiguration task) {
        if (task.type() != TaskType.CODE || task.status() != TaskStatus.ACTIVE
            || task.programmingConfig() == null || task.testCases().isEmpty()) {
            throw new RunCodeException(Reason.TASK_NOT_EXECUTABLE);
        }
        if (!task.programmingConfig().executionEnabled()) {
            throw new RunCodeException(Reason.TASK_EXECUTION_DISABLED);
        }
    }

    private void requireOwnedHomeworkContext(
        UUID studentId,
        TaskQuery.TaskContext task,
        UUID homeworkItemId
    ) {
        HomeworkQuery.StudentTaskContext homework = homeworkQuery
            .findStudentTaskContext(homeworkItemId)
            .orElseThrow(() -> new RunCodeException(Reason.EXECUTION_CONTEXT_INVALID));
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(homework.studentProgramId())
            .orElseThrow(() -> new RunCodeException(Reason.EXECUTION_CONTEXT_INVALID));
        if (!studentProgram.belongsToStudent(studentId)
            || !studentProgram.isAssignedBy(homework.assignedByTeacherId())
            || !homework.taskId().equals(task.id())
            || !task.teacherId().equals(homework.assignedByTeacherId())
            || !task.subjectId().equals(studentProgram.subjectId())
            || homework.homeworkStatus() == HomeworkStatus.CANCELLED) {
            throw new RunCodeException(Reason.EXECUTION_CONTEXT_INVALID);
        }
    }

    private RunCodeResult toStudentResult(
        ExecutionResult result,
        List<TaskTestCase> configuredTests
    ) {
        if (result.status() == ExecutionStatus.SYSTEM_ERROR) {
            return new RunCodeResult(
                result.executionId(), ExecutionStatus.SYSTEM_ERROR, 0, configuredTests.size(),
                0, null, null, List.of()
            );
        }
        Map<UUID, ExecutionTestResult> resultsById = new HashMap<>();
        result.testResults().forEach(test -> resultsById.put(test.testCaseId(), test));
        List<RunCodeResult.TestResult> safeTests = configuredTests.stream().map(test -> {
            ExecutionTestResult testResult = resultsById.get(test.id());
            boolean passed = testResult != null && testResult.passed();
            if (test.hidden()) {
                return new RunCodeResult.TestResult(passed, true, null, null, null);
            }
            return new RunCodeResult.TestResult(
                passed, false, test.inputText(), test.expectedOutput(),
                testResult == null ? null : bounded(testResult.stdoutExcerpt())
            );
        }).toList();
        return new RunCodeResult(
            result.executionId(), result.status(), result.passedTests(), result.totalTests(),
            result.executionTimeMs(), bounded(result.stdoutExcerpt()), bounded(result.stderrExcerpt()),
            safeTests
        );
    }

    private String bounded(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_RESPONSE_OUTPUT_BYTES) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        int usedBytes = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int bytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + bytes > MAX_RESPONSE_OUTPUT_BYTES) {
                break;
            }
            result.append(character);
            usedBytes += bytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
