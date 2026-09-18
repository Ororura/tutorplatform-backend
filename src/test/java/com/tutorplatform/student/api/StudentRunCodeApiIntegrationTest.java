package com.tutorplatform.student.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.execution.application.*;
import com.tutorplatform.homework.domain.*;
import com.tutorplatform.program.domain.learningprogram.*;
import com.tutorplatform.program.domain.studentprogram.*;
import com.tutorplatform.student.domain.*;
import com.tutorplatform.student.infrastructure.persistence.*;
import com.tutorplatform.subject.domain.*;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentRunCodeApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_student_run_code_api", "008");
    }

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
    @Autowired private EntityManager entityManager;
    @MockitoBean private ExecutionPort executionPort;

    @Test
    void runUsesBackendConfigurationSanitizesHiddenTestsAndDoesNotPersist() throws Exception {
        Fixture fixture = createFixture("success", TaskType.CODE, TaskStatus.ACTIVE, true);
        long submissionsBefore = count("submissions");
        long codeSubmissionsBefore = count("code_submissions");
        long progressBefore = count("student_topic_progress");
        when(executionPort.execute(any())).thenAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            return new ExecutionResult(
                request.executionId(), ExecutionStatus.FAILED, 1, 2, 42,
                "combined", "", List.of(
                new ExecutionTestResult(fixture.publicTestId(), true, 20, "5", null),
                new ExecutionTestResult(fixture.hiddenTestId(), false, 22, "secret answer", "secret")
            ));
        });

        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", fixture.taskId())
                .with(user(fixture.studentPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "sourceCode", "print(sum(map(int,input().split())))",
                    "homeworkItemId", fixture.homeworkItemId()
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.passedTests").value(1))
            .andExpect(jsonPath("$.totalTests").value(2))
            .andExpect(jsonPath("$.stdoutExcerpt").doesNotExist())
            .andExpect(jsonPath("$.stderrExcerpt").doesNotExist())
            .andExpect(jsonPath("$.tests[0].position").value(0))
            .andExpect(jsonPath("$.tests[0].hidden").value(false))
            .andExpect(jsonPath("$.tests[0].passed").value(true))
            .andExpect(jsonPath("$.tests[1].hidden").value(true))
            .andExpect(jsonPath("$.tests[1].passed").value(false))
            .andExpect(jsonPath("$..input").doesNotExist())
            .andExpect(jsonPath("$..expectedOutput").doesNotExist())
            .andExpect(jsonPath("$..actualOutput").doesNotExist());

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(executionPort).execute(requestCaptor.capture());
        ExecutionRequest request = requestCaptor.getValue();
        assertThat(request.language()).isEqualTo(ExecutionLanguage.PYTHON);
        assertThat(request.timeLimitMs()).isEqualTo(900);
        assertThat(request.memoryLimitMb()).isEqualTo(128);
        assertThat(request.testCases()).hasSize(2);
        assertThat(count("submissions")).isEqualTo(submissionsBefore);
        assertThat(count("code_submissions")).isEqualTo(codeSubmissionsBefore);
        assertThat(count("student_topic_progress")).isEqualTo(progressBefore);
        assertThat(homeworkRepository.findById(fixture.homeworkId()).orElseThrow().getStatus())
            .isEqualTo(HomeworkStatus.ASSIGNED);
    }

    @Test
    void ownershipTaskStatusTypeAndExecutionFlagAreEnforced() throws Exception {
        Fixture owner = createFixture("owner", TaskType.CODE, TaskStatus.ACTIVE, true);
        Fixture foreign = createFixture("foreign", TaskType.CODE, TaskStatus.ACTIVE, true);
        performRun(owner.studentPrincipal(), foreign.taskId(), foreign.homeworkItemId())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("EXECUTION_CONTEXT_INVALID"));

        performRun(owner.studentPrincipal(), UUID.randomUUID(), owner.homeworkItemId())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));

        Fixture text = createFixture("text", TaskType.TEXT, TaskStatus.ACTIVE, true);
        performRun(text.studentPrincipal(), text.taskId(), text.homeworkItemId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_NOT_EXECUTABLE"));

        Fixture archived = createFixture("archived", TaskType.CODE, TaskStatus.ARCHIVED, true);
        performRun(archived.studentPrincipal(), archived.taskId(), archived.homeworkItemId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_NOT_EXECUTABLE"));

        Fixture disabled = createFixture("disabled", TaskType.CODE, TaskStatus.ACTIVE, false);
        performRun(disabled.studentPrincipal(), disabled.taskId(), disabled.homeworkItemId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_EXECUTION_DISABLED"));
        verifyNoInteractions(executionPort);
    }

    @Test
    void authenticationRoleAndCsrfAreEnforced() throws Exception {
        Fixture fixture = createFixture("security", TaskType.CODE, TaskStatus.ACTIVE, true);
        String body = requestBody(fixture.homeworkItemId());
        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", fixture.taskId())
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        AuthenticatedUser teacher = new AuthenticatedUser(
            UUID.randomUUID(), "teacher@example.com", "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", fixture.taskId())
                .with(user(teacher)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", fixture.taskId())
                .with(user(fixture.studentPrincipal()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        verifyNoInteractions(executionPort);
    }

    @Test
    void runtimeSettingsCannotBeSuppliedByStudent() throws Exception {
        Fixture fixture = createFixture("input", TaskType.CODE, TaskStatus.ACTIVE, true);
        String body = """
            {"sourceCode":"pass","homeworkItemId":"%s","language":"PYTHON",
             "timeLimitMs":30000,"memoryLimitMb":1024,"testCases":[]}
            """.formatted(fixture.homeworkItemId());

        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", fixture.taskId())
                .with(user(fixture.studentPrincipal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(executionPort);
    }

    @Test
    void openApiPublishesOnlyPublicRunSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/student/tasks/{taskId}/run'].post.operationId")
                .value("runCode"))
            .andExpect(jsonPath("$.components.schemas.RunCodeRequest.properties.sourceCode").exists())
            .andExpect(jsonPath("$.components.schemas.RunCodeRequest.properties.homeworkItemId.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.RunCodeRequest.properties.language").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.RunCodeResponse").exists())
            .andExpect(jsonPath("$.components.schemas.StudentRunCodeTestResultResponse").exists())
            .andExpect(jsonPath("$.components.schemas.StudentRunCodeTestResultResponse.properties.input").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.StudentRunCodeTestResultResponse.properties.expectedOutput").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.StudentRunCodeTestResultResponse.properties.actualOutput").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.TaskTestCaseResponse.properties.inputText").exists())
            .andExpect(jsonPath("$.components.schemas.TaskTestCaseResponse.properties.expectedOutput").exists())
            .andExpect(jsonPath("$.components.schemas.ExecutionStatus.enum.length()").value(5))
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private org.springframework.test.web.servlet.ResultActions performRun(
        AuthenticatedUser principal,
        UUID taskId,
        UUID homeworkItemId
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/student/tasks/{taskId}/run", taskId)
            .with(user(principal)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(requestBody(homeworkItemId)));
    }

    private String requestBody(UUID homeworkItemId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "sourceCode", "pass", "homeworkItemId", homeworkItemId
        ));
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table)
            .getSingleResult()).longValue();
    }

    private Fixture createFixture(
        String label,
        TaskType taskType,
        TaskStatus taskStatus,
        boolean executionEnabled
    ) {
        UserEntity teacherUser = new UserEntity(
            UUID.randomUUID(), label + "-teacher-" + UUID.randomUUID() + "@example.com",
            "hash", UserStatus.ACTIVE
        );
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), teacherUser, "Teacher"
        ));
        UserEntity studentUser = new UserEntity(
            UUID.randomUUID(), label + "-student-" + UUID.randomUUID() + "@example.com",
            "hash", UserStatus.ACTIVE
        );
        studentUser.addRole(UserRole.STUDENT);
        userRepository.saveAndFlush(studentUser);
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), studentUser.id(), "Student", null, StudentStatus.ACTIVE, null, null
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Subject " + UUID.randomUUID(), null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Program", null,
            LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Task", "Description",
            taskType, TaskDifficulty.EASY, taskStatus
        ));
        UUID publicTestId = UUID.randomUUID();
        UUID hiddenTestId = UUID.randomUUID();
        if (taskType == TaskType.CODE) {
            programmingConfigRepository.saveAndFlush(new ProgrammingTaskConfig(
                task.getId(), ProgrammingLanguage.PYTHON, "starter", executionEnabled, 900, 128
            ));
            testCaseRepository.saveAllAndFlush(List.of(
                new TaskTestCase(publicTestId, task.getId(), "2 3", "5", false,
                    ComparisonMode.NORMALIZED, 0),
                new TaskTestCase(hiddenTestId, task.getId(), "secret input", "secret answer", true,
                    ComparisonMode.EXACT, 1)
            ));
        }
        UUID homeworkId = UUID.randomUUID();
        UUID homeworkItemId = UUID.randomUUID();
        homeworkRepository.saveAndFlush(new HomeworkEntity(
            homeworkId, studentProgram.id(), teacher.id(), "Homework", null, Instant.now(), null,
            HomeworkStatus.ASSIGNED, null,
            List.of(new HomeworkItemEntity(homeworkItemId, homeworkId, task.getId(), 0, true))
        ));
        return new Fixture(
            new AuthenticatedUser(
                studentUser.id(), studentUser.email(), "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
            ), task.getId(), homeworkId, homeworkItemId, publicTestId, hiddenTestId
        );
    }

    private record Fixture(
        AuthenticatedUser studentPrincipal,
        UUID taskId,
        UUID homeworkId,
        UUID homeworkItemId,
        UUID publicTestId,
        UUID hiddenTestId
    ) {
    }
}
