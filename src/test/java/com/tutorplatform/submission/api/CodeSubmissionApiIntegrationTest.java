package com.tutorplatform.submission.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.homework.domain.*;
import com.tutorplatform.program.domain.learningprogram.*;
import com.tutorplatform.program.domain.studentprogram.*;
import com.tutorplatform.student.domain.*;
import com.tutorplatform.student.infrastructure.persistence.*;
import com.tutorplatform.subject.domain.*;
import com.tutorplatform.submission.domain.*;
import com.tutorplatform.submission.application.CodeSubmissionTransactions;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CodeSubmissionApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProgrammingTaskConfigRepository programmingConfigRepository;
    @Autowired private TaskTestCaseRepository testCaseRepository;
    @Autowired private HomeworkRepository homeworkRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private CodeSubmissionRepository codeSubmissionRepository;
    @Autowired private CodeSubmissionTransactions codeSubmissionTransactions;
    @Autowired private EntityManager entityManager;
    @MockitoBean private ExecutionPort executionPort;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
    }

    @Test
    void submitPersistsServerOwnedCodeResultAndCompletesHomework() throws Exception {
        Fixture fixture = createFixture("passed", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.ASSIGNED);
        when(executionPort.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive()).isFalse();
            assertThat(request.timeLimitMs()).isEqualTo(900);
            assertThat(request.memoryLimitMb()).isEqualTo(128);
            assertThat(request.testCases()).hasSize(2);
            return new ExecutionResult(
                request.executionId(), ExecutionStatus.PASSED, 2, 2, 42,
                "ok", null, List.of()
            );
        });

        String response = submit(fixture, "print(42)")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.attemptNo").value(1))
            .andExpect(jsonPath("$.status").value("PASSED"))
            .andExpect(jsonPath("$.textAnswer").doesNotExist())
            .andExpect(jsonPath("$.execution.status").value("PASSED"))
            .andExpect(jsonPath("$.execution.passedTests").value(2))
            .andExpect(jsonPath("$.execution.totalTests").value(2))
            .andExpect(jsonPath("$.sourceCode").value("print(42)"))
            .andExpect(jsonPath("$.studentId").doesNotExist())
            .andExpect(jsonPath("$..inputText").doesNotExist())
            .andExpect(jsonPath("$..expectedOutput").doesNotExist())
            .andReturn().getResponse().getContentAsString();

        UUID submissionId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        SubmissionEntity submission = submissionRepository.findById(submissionId).orElseThrow();
        CodeSubmissionEntity code = codeSubmissionRepository.findBySubmissionId(submissionId).orElseThrow();
        assertThat(submission.getStudentId()).isEqualTo(fixture.studentId());
        assertThat(submission.getStudentProgramId()).isEqualTo(fixture.studentProgramId());
        assertThat(submission.getTextAnswer()).isNull();
        assertThat(submission.getSubmittedAt()).isNotNull();
        assertThat(code.getSourceCode()).isEqualTo("print(42)");
        assertThat(homeworkRepository.findById(fixture.homeworkId()).orElseThrow().getStatus())
            .isEqualTo(HomeworkStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/student/tasks/{taskId}/submissions", fixture.taskId())
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].execution.status").value("PASSED"))
            .andExpect(jsonPath("$.items[0].sourceCode").doesNotExist());
        mockMvc.perform(get("/api/v1/student/submissions/{id}", submissionId)
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceCode").value("print(42)"));
    }

    @Test
    void workerFailureKeepsSubmissionAndReturnsSafeSystemError() throws Exception {
        Fixture fixture = createFixture("system", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.ASSIGNED);
        when(executionPort.execute(any())).thenThrow(new IllegalStateException("internal host secret"));

        String response = submit(fixture, "broken")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SYSTEM_ERROR"))
            .andExpect(jsonPath("$.execution.status").value("SYSTEM_ERROR"))
            .andExpect(jsonPath("$.execution.stderrExcerpt").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("internal host secret")
            )))
            .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        assertThat(submissionRepository.findById(id)).get()
            .extracting(SubmissionEntity::getStatus).isEqualTo(SubmissionStatus.SYSTEM_ERROR);
        assertThat(codeSubmissionRepository.findBySubmissionId(id)).get()
            .extracting(CodeSubmissionEntity::getExecutionStatus)
            .isEqualTo(CodeExecutionStatus.SYSTEM_ERROR);
        assertThat(homeworkRepository.findById(fixture.homeworkId()).orElseThrow().getStatus())
            .isEqualTo(HomeworkStatus.ASSIGNED);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionStatus.class, names = {"FAILED", "TIMEOUT", "RUNTIME_ERROR"})
    void userCodeFailureStatusesMapToFailedSubmission(ExecutionStatus executionStatus) throws Exception {
        Fixture fixture = createFixture(
            "failure-" + executionStatus, TaskType.CODE, TaskStatus.ACTIVE, true,
            HomeworkStatus.ASSIGNED
        );
        when(executionPort.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            return new ExecutionResult(
                request.executionId(), executionStatus, 1, 2, 25,
                "student output", null, List.of()
            );
        });

        String response = submit(fixture, "solution")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.execution.status").value(executionStatus.name()))
            .andExpect(jsonPath("$.execution.passedTests").value(1))
            .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        assertThat(submissionRepository.findById(id)).get()
            .extracting(SubmissionEntity::getStatus).isEqualTo(SubmissionStatus.FAILED);
        assertThat(codeSubmissionRepository.findBySubmissionId(id)).get()
            .extracting(CodeSubmissionEntity::getExecutionStatus)
            .isEqualTo(CodeExecutionStatus.valueOf(executionStatus.name()));
        assertThat(homeworkRepository.findById(fixture.homeworkId()).orElseThrow().getStatus())
            .isEqualTo(HomeworkStatus.ASSIGNED);
    }

    @Test
    void initialAndFinalPersistencePairsAreAtomic() {
        Fixture fixture = createFixture(
            "atomic", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.ASSIGNED
        );
        long submissionsBefore = count("submissions");
        assertThatThrownBy(() -> codeSubmissionTransactions.createPending(
            fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
            fixture.homeworkItemId(), null, 2
        )).isInstanceOf(NullPointerException.class);
        assertThat(count("submissions")).isEqualTo(submissionsBefore);

        var pending = codeSubmissionTransactions.createPending(
            fixture.studentId(), fixture.studentProgramId(), fixture.taskId(),
            fixture.homeworkItemId(), "solution", 2
        );
        assertThatThrownBy(() -> codeSubmissionTransactions.finish(
            pending.id(), CodeExecutionStatus.FAILED, 3, 2, 10, null, null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThat(submissionRepository.findById(pending.id())).get()
            .extracting(SubmissionEntity::getStatus).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(codeSubmissionRepository.findBySubmissionId(pending.id())).get()
            .extracting(CodeSubmissionEntity::getExecutionStatus)
            .isEqualTo(CodeExecutionStatus.PENDING);
    }

    @Test
    void invalidTaskAndHomeworkStatesAreRejectedBeforePersistence() throws Exception {
        Fixture text = createFixture("text", TaskType.TEXT, TaskStatus.ACTIVE, true, HomeworkStatus.ASSIGNED);
        submit(text, "pass").andExpect(status().isBadRequest());
        Fixture disabled = createFixture("disabled", TaskType.CODE, TaskStatus.ACTIVE, false, HomeworkStatus.ASSIGNED);
        submit(disabled, "pass").andExpect(status().isBadRequest());
        Fixture cancelled = createFixture("cancelled", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.CANCELLED);
        submit(cancelled, "pass").andExpect(status().isConflict());
        Fixture completed = createFixture("completed", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.COMPLETED);
        submit(completed, "pass").andExpect(status().isConflict());
        Fixture serverOwned = createFixture(
            "server-owned", TaskType.CODE, TaskStatus.ACTIVE, true, HomeworkStatus.ASSIGNED
        );
        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", serverOwned.taskId())
                .with(user(serverOwned.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"homeworkItemId":"%s","sourceCode":"pass","attemptNo":99,
                     "status":"PASSED","language":"PYTHON","testCases":[]}
                    """.formatted(serverOwned.homeworkItemId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details[0].field").value("request"));
        verifyNoInteractions(executionPort);
    }

    private org.springframework.test.web.servlet.ResultActions submit(Fixture fixture, String sourceCode)
        throws Exception {
        return mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", fixture.taskId())
            .with(user(fixture.principal())).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "homeworkItemId", fixture.homeworkItemId(), "sourceCode", sourceCode
            ))));
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table)
            .getSingleResult()).longValue();
    }

    private Fixture createFixture(
        String label,
        TaskType taskType,
        TaskStatus taskStatus,
        boolean executionEnabled,
        HomeworkStatus homeworkStatus
    ) {
        UserEntity teacherUser = new UserEntity(
            UUID.randomUUID(), label + "-teacher-" + UUID.randomUUID() + "@example.com", "hash", UserStatus.ACTIVE
        );
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), teacherUser, "Teacher"
        ));
        UserEntity studentUser = new UserEntity(
            UUID.randomUUID(), label + "-student-" + UUID.randomUUID() + "@example.com", "hash", UserStatus.ACTIVE
        );
        studentUser.addRole(UserRole.STUDENT);
        userRepository.saveAndFlush(studentUser);
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), studentUser.id(), "Student", null, StudentStatus.ACTIVE, null, null
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Subject", null, SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Program", null, LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Task", "Description",
            taskType, TaskDifficulty.EASY, taskStatus
        ));
        if (taskType == TaskType.CODE) {
            programmingConfigRepository.saveAndFlush(new ProgrammingTaskConfig(
                task.getId(), ProgrammingLanguage.PYTHON, null, executionEnabled, 900, 128
            ));
            testCaseRepository.saveAllAndFlush(List.of(
                new TaskTestCase(UUID.randomUUID(), task.getId(), "1", "1", false, ComparisonMode.EXACT, 0),
                new TaskTestCase(UUID.randomUUID(), task.getId(), "hidden", "secret", true, ComparisonMode.EXACT, 1)
            ));
        }
        UUID homeworkId = UUID.randomUUID();
        UUID homeworkItemId = UUID.randomUUID();
        homeworkRepository.saveAndFlush(new HomeworkEntity(
            homeworkId, studentProgram.id(), teacher.id(), "Homework", null, Instant.now(), null,
            homeworkStatus,
            homeworkStatus == HomeworkStatus.COMPLETED ? Instant.now() : null,
            List.of(new HomeworkItemEntity(homeworkItemId, homeworkId, task.getId(), 0, true))
        ));
        return new Fixture(
            new AuthenticatedUser(
                studentUser.id(), studentUser.email(), "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
            ), student.getId(), studentProgram.id(), task.getId(), homeworkId, homeworkItemId
        );
    }

    private record Fixture(
        AuthenticatedUser principal,
        UUID studentId,
        UUID studentProgramId,
        UUID taskId,
        UUID homeworkId,
        UUID homeworkItemId
    ) {
    }
}
