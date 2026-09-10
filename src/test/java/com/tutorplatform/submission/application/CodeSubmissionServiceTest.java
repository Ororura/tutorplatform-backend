package com.tutorplatform.submission.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.StudentOwnershipQuery;
import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.task.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeSubmissionServiceTest {

    private final StudentOwnershipQuery ownership = mock(StudentOwnershipQuery.class);
    private final TaskQuery tasks = mock(TaskQuery.class);
    private final SubmissionHomeworkContextQuery homework = mock(SubmissionHomeworkContextQuery.class);
    private final ProgramQuery programs = mock(ProgramQuery.class);
    private final CodeSubmissionTransactions transactions = mock(CodeSubmissionTransactions.class);
    private final ExecutionPort execution = mock(ExecutionPort.class);
    private final CodeSubmissionService service = new CodeSubmissionService(
        ownership, tasks, homework, programs, transactions, execution
    );

    private final UUID userId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final UUID studentProgramId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID homeworkItemId = UUID.randomUUID();
    private final UUID testId = UUID.randomUUID();
    private final AuthenticatedUser principal = new AuthenticatedUser(
        userId, "student@example.com", "hash", true,
        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
    );

    @BeforeEach
    void arrangeContext() {
        when(ownership.findStudentIdByUserId(userId)).thenReturn(Optional.of(studentId));
        when(tasks.findTask(taskId)).thenReturn(Optional.of(new TaskQuery.TaskContext(
            taskId, teacherId, subjectId, "Code", TaskType.CODE, TaskStatus.ACTIVE
        )));
        when(homework.findSubmissionContext(homeworkItemId)).thenReturn(Optional.of(
            new SubmissionHomeworkContextQuery.HomeworkSubmissionContext(
                UUID.randomUUID(), studentProgramId, teacherId, false, false,
                homeworkItemId, taskId
            )
        ));
        when(programs.findStudentProgram(studentProgramId)).thenReturn(Optional.of(
            new ProgramQuery.StudentProgramContext(
                studentProgramId, studentId, UUID.randomUUID(), teacherId, subjectId, 480
            )
        ));
        when(tasks.findCodeTaskConfiguration(taskId)).thenReturn(Optional.of(configuration(true)));
        when(transactions.createPending(
            eq(studentId), eq(studentProgramId), eq(taskId), eq(homeworkItemId), anyString(), eq(1)
        )).thenReturn(result(SubmissionStatus.SUBMITTED));
        when(transactions.finish(any(), any(), anyInt(), anyInt(), anyLong(), any(), any()))
            .thenAnswer(invocation -> result(switch ((CodeExecutionStatus) invocation.getArgument(1)) {
                case PASSED -> SubmissionStatus.PASSED;
                case SYSTEM_ERROR -> SubmissionStatus.SYSTEM_ERROR;
                default -> SubmissionStatus.FAILED;
            }));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionStatus.class)
    void persistsEveryExecutionOutcomeAndUsesServerConfiguration(ExecutionStatus status) {
        when(execution.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            int passed = status == ExecutionStatus.PASSED ? 1 : 0;
            return new ExecutionResult(
                request.executionId(), status, passed, 1, 77,
                "stdout", "stderr", List.of()
            );
        });

        service.submit(principal, taskId, homeworkItemId, "print(42)");

        InOrder order = inOrder(transactions, execution);
        order.verify(transactions).createPending(
            studentId, studentProgramId, taskId, homeworkItemId, "print(42)", 1
        );
        ArgumentCaptor<ExecutionRequest> request = ArgumentCaptor.forClass(ExecutionRequest.class);
        order.verify(execution).execute(request.capture());
        CodeExecutionStatus expectedExecutionStatus = CodeExecutionStatus.valueOf(status.name());
        int expectedPassed = status == ExecutionStatus.PASSED ? 1 : 0;
        long expectedTime = status == ExecutionStatus.SYSTEM_ERROR ? 0 : 77;
        order.verify(transactions).finish(
            any(), eq(expectedExecutionStatus), eq(expectedPassed), eq(1), eq(expectedTime),
            status == ExecutionStatus.SYSTEM_ERROR ? isNull() : eq("stdout"),
            status == ExecutionStatus.SYSTEM_ERROR ? isNull() : eq("stderr")
        );
        assertThat(request.getValue().language()).isEqualTo(ExecutionLanguage.PYTHON);
        assertThat(request.getValue().timeLimitMs()).isEqualTo(900);
        assertThat(request.getValue().memoryLimitMb()).isEqualTo(128);
        assertThat(request.getValue().testCases()).singleElement().satisfies(test -> {
            assertThat(test.id()).isEqualTo(testId);
            assertThat(test.inputText()).isEqualTo("hidden input");
            assertThat(test.expectedOutput()).isEqualTo("hidden output");
        });
    }

    @Test
    void infrastructureFailureFinalizesExistingSubmissionAsSystemError() {
        when(execution.execute(any())).thenThrow(new IllegalStateException("worker secret"));

        service.submit(principal, taskId, homeworkItemId, "pass");

        verify(transactions).createPending(
            studentId, studentProgramId, taskId, homeworkItemId, "pass", 1
        );
        verify(transactions).finish(
            any(), eq(CodeExecutionStatus.SYSTEM_ERROR), eq(0), eq(1), eq(0L),
            isNull(), isNull()
        );
    }

    @Test
    void executionCanObserveThatNoSpringTransactionWrapsWorkerCall() {
        when(execution.execute(any())).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive()).isFalse();
            ExecutionRequest request = invocation.getArgument(0);
            return ExecutionResult.systemError(request.executionId(), 1);
        });

        service.submit(principal, taskId, homeworkItemId, "pass");
    }

    private TaskQuery.CodeTaskConfiguration configuration(boolean executionEnabled) {
        return new TaskQuery.CodeTaskConfiguration(
            taskId, TaskType.CODE, TaskStatus.ACTIVE,
            new ProgrammingTaskConfig(
                taskId, ProgrammingLanguage.PYTHON, null, executionEnabled, 900, 128
            ),
            List.of(new TaskTestCase(
                testId, taskId, "hidden input", "hidden output", true,
                ComparisonMode.EXACT, 0
            ))
        );
    }

    private SubmissionResult result(SubmissionStatus status) {
        return new SubmissionResult(
            UUID.randomUUID(), studentId, taskId, homeworkItemId, 1, status,
            null, Instant.now(), null
        );
    }
}
