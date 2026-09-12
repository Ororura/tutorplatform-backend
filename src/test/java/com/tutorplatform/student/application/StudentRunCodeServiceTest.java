package com.tutorplatform.student.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.homework.application.HomeworkQuery;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.coderunner.RunCodeResult;
import com.tutorplatform.student.application.coderunner.StudentRunCodeService;
import com.tutorplatform.student.application.coderunner.RunCodeException;
import com.tutorplatform.student.application.coderunner.RunCodeException.Reason;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentRunCodeServiceTest {

    @Mock private StudentOwnershipQuery studentOwnershipQuery;
    @Mock private ProgramQuery programQuery;
    @Mock private HomeworkQuery homeworkQuery;
    @Mock private TaskQuery taskQuery;
    @Mock private ExecutionPort executionPort;

    private StudentRunCodeService service;
    private UUID userId;
    private UUID studentId;
    private UUID taskId;
    private UUID homeworkItemId;
    private UUID studentProgramId;
    private UUID teacherId;
    private UUID subjectId;
    private TaskTestCase publicTest;
    private TaskTestCase hiddenTest;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        service = new StudentRunCodeService(
            studentOwnershipQuery, programQuery, homeworkQuery, taskQuery, executionPort
        );
        userId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        homeworkItemId = UUID.randomUUID();
        studentProgramId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        publicTest = new TaskTestCase(
            UUID.randomUUID(), taskId, "2 3", "5", false, ComparisonMode.NORMALIZED, 0
        );
        hiddenTest = new TaskTestCase(
            UUID.randomUUID(), taskId, "secret input", "secret answer", true,
            ComparisonMode.EXACT, 1
        );
        principal = new AuthenticatedUser(
            userId, "student@example.com", "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        allowValidContext();
    }

    @Test
    void mapsOnlyServerTaskConfigurationAndGeneratesExecutionId() {
        when(executionPort.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            return new ExecutionResult(
                request.executionId(), ExecutionStatus.FAILED, 1, 2, 120,
                "combined", "", List.of(
                new ExecutionTestResult(publicTest.id(), true, 50, "5", null),
                new ExecutionTestResult(hiddenTest.id(), false, 70, "leak", "hidden error")
            ));
        });

        RunCodeResult result = service.run(principal, taskId, homeworkItemId, "print(input())");

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(executionPort).execute(captor.capture());
        ExecutionRequest request = captor.getValue();
        assertThat(request.executionId()).isNotNull().isEqualTo(result.executionId());
        assertThat(request.language()).isEqualTo(ExecutionLanguage.PYTHON);
        assertThat(request.sourceCode()).isEqualTo("print(input())");
        assertThat(request.timeLimitMs()).isEqualTo(750);
        assertThat(request.memoryLimitMb()).isEqualTo(96);
        assertThat(request.testCases()).extracting(ExecutionTestCase::id)
            .containsExactly(publicTest.id(), hiddenTest.id());
        assertThat(request.testCases()).extracting(ExecutionTestCase::comparisonMode)
            .containsExactly(ExecutionComparisonMode.NORMALIZED, ExecutionComparisonMode.EXACT);
        assertThat(result.status()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.tests().getFirst())
            .isEqualTo(new RunCodeResult.TestResult(true, false, "2 3", "5", "5"));
        assertThat(result.tests().get(1))
            .isEqualTo(new RunCodeResult.TestResult(false, true, null, null, null));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionStatus.class, names = "SYSTEM_ERROR", mode = EnumSource.Mode.EXCLUDE)
    void preservesSolutionExecutionStatuses(ExecutionStatus status) {
        when(executionPort.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            return new ExecutionResult(
                request.executionId(), status, status == ExecutionStatus.PASSED ? 2 : 0,
                2, 10, null, null, List.of()
            );
        });

        assertThat(service.run(principal, taskId, homeworkItemId, "pass").status())
            .isEqualTo(status);
    }

    @Test
    void unavailableWorkerProducesSanitizedSystemError() {
        when(executionPort.execute(any())).thenThrow(new IllegalStateException("docker secret path"));

        RunCodeResult result = service.run(principal, taskId, homeworkItemId, "pass");

        assertThat(result.status()).isEqualTo(ExecutionStatus.SYSTEM_ERROR);
        assertThat(result.stdoutExcerpt()).isNull();
        assertThat(result.stderrExcerpt()).isNull();
        assertThat(result.tests()).isEmpty();
        assertThat(result.totalTests()).isEqualTo(2);
    }

    @Test
    void foreignStudentProgramIsRejectedBeforeExecution() {
        when(programQuery.findStudentProgram(studentProgramId)).thenReturn(Optional.of(
            new ProgramQuery.StudentProgramContext(
                studentProgramId, UUID.randomUUID(), UUID.randomUUID(), teacherId, subjectId, 480
            )
        ));

        assertReason(Reason.EXECUTION_CONTEXT_INVALID);
    }

    @Test
    void taskOutsideHomeworkItemIsRejectedBeforeExecution() {
        when(homeworkQuery.findStudentTaskContext(homeworkItemId)).thenReturn(Optional.of(
            new HomeworkQuery.StudentTaskContext(
                UUID.randomUUID(), studentProgramId, teacherId, HomeworkStatus.ASSIGNED,
                homeworkItemId, UUID.randomUUID()
            )
        ));

        assertReason(Reason.EXECUTION_CONTEXT_INVALID);
    }

    @Test
    void cancelledHomeworkIsRejectedBeforeExecution() {
        when(homeworkQuery.findStudentTaskContext(homeworkItemId)).thenReturn(Optional.of(
            new HomeworkQuery.StudentTaskContext(
                UUID.randomUUID(), studentProgramId, teacherId, HomeworkStatus.CANCELLED,
                homeworkItemId, taskId
            )
        ));

        assertReason(Reason.EXECUTION_CONTEXT_INVALID);
    }

    @Test
    void unknownTaskIsNotFound() {
        when(taskQuery.findTask(taskId)).thenReturn(Optional.empty());

        assertReason(Reason.TASK_NOT_FOUND);
    }

    @Test
    void textAndInactiveTasksAreNotExecutable() {
        when(taskQuery.findCodeTaskConfiguration(taskId)).thenReturn(Optional.of(task(TaskType.TEXT, TaskStatus.ACTIVE, true)));
        assertReason(Reason.TASK_NOT_EXECUTABLE);

        when(taskQuery.findCodeTaskConfiguration(taskId)).thenReturn(Optional.of(task(TaskType.CODE, TaskStatus.ARCHIVED, true)));
        assertReason(Reason.TASK_NOT_EXECUTABLE);
    }

    @Test
    void executionDisabledIsControlled() {
        when(taskQuery.findCodeTaskConfiguration(taskId)).thenReturn(Optional.of(task(TaskType.CODE, TaskStatus.ACTIVE, false)));

        assertReason(Reason.TASK_EXECUTION_DISABLED);
    }

    private void assertReason(Reason reason) {
        assertThatThrownBy(() -> service.run(principal, taskId, homeworkItemId, "pass"))
            .isInstanceOfSatisfying(RunCodeException.class,
                exception -> assertThat(exception.reason()).isEqualTo(reason));
        verifyNoInteractions(executionPort);
    }

    private void allowValidContext() {
        when(studentOwnershipQuery.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(taskQuery.findTask(taskId)).thenReturn(Optional.of(new TaskQuery.TaskContext(
            taskId, teacherId, subjectId, "Task", TaskType.CODE, TaskStatus.ACTIVE
        )));
        when(taskQuery.findCodeTaskConfiguration(taskId)).thenReturn(Optional.of(task(TaskType.CODE, TaskStatus.ACTIVE, true)));
        when(homeworkQuery.findStudentTaskContext(homeworkItemId)).thenReturn(Optional.of(
            new HomeworkQuery.StudentTaskContext(
                UUID.randomUUID(), studentProgramId, teacherId, HomeworkStatus.ASSIGNED,
                homeworkItemId, taskId
            )
        ));
        when(programQuery.findStudentProgram(studentProgramId)).thenReturn(Optional.of(
            new ProgramQuery.StudentProgramContext(
                studentProgramId, studentId, UUID.randomUUID(), teacherId, subjectId, 480
            )
        ));
    }

    private TaskQuery.CodeTaskConfiguration task(
        TaskType type,
        TaskStatus status,
        boolean executionEnabled
    ) {
        return new TaskQuery.CodeTaskConfiguration(
            taskId, type, status,
            new ProgrammingTaskConfig(
                taskId, ProgrammingLanguage.PYTHON, "starter", executionEnabled, 750, 96
            ),
            List.of(publicTest, hiddenTest)
        );
    }
}
