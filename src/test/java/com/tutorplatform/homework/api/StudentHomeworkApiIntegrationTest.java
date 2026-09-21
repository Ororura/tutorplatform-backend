package com.tutorplatform.homework.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StudentHomeworkApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProgrammingTaskConfigRepository programmingConfigRepository;
    @Autowired private TaskTestCaseRepository taskTestCaseRepository;
    @Autowired private HomeworkRepository homeworkRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_student_homework_api", "008");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Test
    void authenticationAndRoleAreEnforced() throws Exception {
        mockMvc.perform(get("/api/v1/student/homeworks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        AuthenticatedUser teacher =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        "teacher-role@example.com",
                        "hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        mockMvc.perform(get("/api/v1/student/homeworks").with(user(teacher)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        Fixture fixture = createFixture("auth");
        mockMvc.perform(get("/api/v1/student/homeworks").with(user(fixture.studentPrincipal())))
                .andExpect(status().isOk());
    }

    @Test
    void listReturnsOnlyCurrentStudentHomeworkAndSeveralRows() throws Exception {
        Fixture owner = createFixture("list-owner");
        Fixture foreign = createFixture("list-foreign");
        HomeworkEntity first =
                createHomework(
                        owner,
                        "First",
                        Instant.now().minusSeconds(20),
                        Instant.now().plusSeconds(3600),
                        HomeworkStatus.ASSIGNED,
                        null,
                        owner.tasks().subList(0, 2));
        HomeworkEntity second =
                createHomework(
                        owner,
                        "Second",
                        Instant.now().minusSeconds(10),
                        null,
                        HomeworkStatus.ASSIGNED,
                        null,
                        List.of(owner.tasks().getFirst()));
        createHomework(
                foreign,
                "Foreign",
                Instant.now(),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                foreign.tasks());

        mockMvc.perform(get("/api/v1/student/homeworks").with(user(owner.studentPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(second.getId().toString()))
                .andExpect(jsonPath("$.items[1].id").value(first.getId().toString()))
                .andExpect(jsonPath("$.items[0].itemsCount").value(1))
                .andExpect(jsonPath("$.items[1].itemsCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void paginationAndSortingAreAppliedInDatabase() throws Exception {
        Fixture fixture = createFixture("page-sort");
        createHomework(
                fixture,
                "Zulu",
                Instant.now().minusSeconds(30),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "Alpha",
                Instant.now().minusSeconds(20),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "Mike",
                Instant.now().minusSeconds(10),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());

        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Mike"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void statusAndStudentProgramFiltersAreOwnershipScoped() throws Exception {
        Fixture fixture = createFixture("filters");
        StudentProgramEntity otherProgram = createStudentProgram(fixture);
        createHomework(fixture, fixture.studentProgram(), "Assigned", HomeworkStatus.ASSIGNED);
        createHomework(fixture, fixture.studentProgram(), "Cancelled", HomeworkStatus.CANCELLED);
        createHomework(fixture, otherProgram, "Other program", HomeworkStatus.ASSIGNED);

        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("studentProgramId", fixture.studentProgram().id().toString())
                                .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Assigned"));

        Fixture foreign = createFixture("foreign-program");
        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param(
                                        "studentProgramId",
                                        foreign.studentProgram().id().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void invalidSortAndPaginationAreRejected() throws Exception {
        Fixture fixture = createFixture("invalid-list");
        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("sort", "assignedByTeacherId,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("sort"));
        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("size"));
    }

    @Test
    void overdueIsDerivedFromDueDateLifecycleAndCompletion() throws Exception {
        Fixture fixture = createFixture("overdue");
        Instant past = Instant.now().minusSeconds(3600);
        Instant future = Instant.now().plusSeconds(3600);
        createHomework(
                fixture,
                "Past assigned",
                Instant.now(),
                past,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "Future",
                Instant.now(),
                future,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "No due",
                Instant.now(),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "Cancelled",
                Instant.now(),
                past,
                HomeworkStatus.CANCELLED,
                null,
                fixture.tasks());
        createHomework(
                fixture,
                "Completed",
                Instant.now(),
                past,
                HomeworkStatus.COMPLETED,
                Instant.now().minusSeconds(30),
                fixture.tasks());

        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.title == 'Past assigned')].overdue").value(true))
                .andExpect(jsonPath("$.items[?(@.title == 'Future')].overdue").value(false))
                .andExpect(jsonPath("$.items[?(@.title == 'No due')].overdue").value(false))
                .andExpect(jsonPath("$.items[?(@.title == 'Cancelled')].overdue").value(false))
                .andExpect(jsonPath("$.items[?(@.title == 'Completed')].overdue").value(false));
    }

    @Test
    void listPublishesPersistedCompletedAt() throws Exception {
        Fixture fixture = createFixture("completed-at");
        Instant completedAt = Instant.parse("2026-02-03T04:05:06Z");
        createHomework(
                fixture,
                "Completed",
                Instant.now(),
                null,
                HomeworkStatus.COMPLETED,
                completedAt,
                fixture.tasks());
        createHomework(
                fixture,
                "Assigned",
                Instant.now(),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                fixture.tasks());

        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Assigned"))
                .andExpect(
                        jsonPath("$.items[0].completedAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.items[1].title").value("Completed"))
                .andExpect(jsonPath("$.items[1].completedAt").value("2026-02-03T04:05:06Z"));
    }

    @Test
    void detailReturnsOrderedItemsAndStudentTaskProjectionOnly() throws Exception {
        Fixture fixture = createFixture("detail");
        HomeworkEntity homework =
                createHomework(
                        fixture,
                        "Detailed",
                        Instant.now(),
                        Instant.now().plusSeconds(3600),
                        HomeworkStatus.ASSIGNED,
                        null,
                        List.of(fixture.tasks().get(1), fixture.tasks().getFirst()));
        createHomework(
                fixture,
                "Another",
                Instant.now(),
                null,
                HomeworkStatus.ASSIGNED,
                null,
                List.of(fixture.tasks().get(2)));

        mockMvc.perform(
                        get("/api/v1/student/homeworks/{homeworkId}", homework.getId())
                                .with(user(fixture.studentPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(homework.getId().toString()))
                .andExpect(jsonPath("$.description").value("Homework description"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].position").value(0))
                .andExpect(
                        jsonPath("$.items[0].taskId")
                                .value(fixture.tasks().get(1).getId().toString()))
                .andExpect(
                        jsonPath("$.items[0].task.id")
                                .value(fixture.tasks().get(1).getId().toString()))
                .andExpect(jsonPath("$.items[0].task.title").value("Task 1"))
                .andExpect(jsonPath("$.items[0].task.descriptionMarkdown").value("Description 1"))
                .andExpect(jsonPath("$.items[0].task.taskType").value("TEXT"))
                .andExpect(jsonPath("$.items[0].task.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.items[1].position").value(1))
                .andExpect(
                        jsonPath("$.items[?(@.taskId == '" + fixture.tasks().get(2).getId() + "')]")
                                .isEmpty())
                .andExpect(jsonPath("$.assignedByTeacherId").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.items[0].task.teacherId").doesNotExist())
                .andExpect(jsonPath("$.items[0].task.subjectId").doesNotExist())
                .andExpect(jsonPath("$.items[0].task.status").doesNotExist());
    }

    @Test
    void detailPublishesSafeCodeMetadataAndBatchedItemSubmissionState() throws Exception {
        Fixture fixture = createFixture("execution-state");
        TaskEntity codeTask =
                taskRepository.saveAndFlush(
                        new TaskEntity(
                                UUID.randomUUID(),
                                fixture.teacher().id(),
                                fixture.subject().id(),
                                "Code task",
                                "Solve it",
                                TaskType.CODE,
                                TaskDifficulty.HARD,
                                TaskStatus.ACTIVE));
        programmingConfigRepository.saveAndFlush(
                new ProgrammingTaskConfig(
                        codeTask.getId(),
                        ProgrammingLanguage.PYTHON,
                        "print('starter')",
                        true,
                        1200,
                        192));
        taskTestCaseRepository.saveAllAndFlush(
                List.of(
                        new TaskTestCase(
                                UUID.randomUUID(),
                                codeTask.getId(),
                                "visible",
                                "answer",
                                false,
                                ComparisonMode.EXACT,
                                0),
                        new TaskTestCase(
                                UUID.randomUUID(),
                                codeTask.getId(),
                                "secret",
                                "secret answer",
                                true,
                                ComparisonMode.EXACT,
                                1)));
        HomeworkEntity homework =
                createHomework(
                        fixture,
                        "Stateful",
                        Instant.now(),
                        null,
                        HomeworkStatus.ASSIGNED,
                        null,
                        List.of(fixture.tasks().getFirst(), fixture.tasks().get(1), codeTask));
        UUID untouchedItem = homework.getItems().get(0).id();
        UUID reviewItem = homework.getItems().get(1).id();
        UUID codeItem = homework.getItems().get(2).id();
        saveSubmission(
                fixture, fixture.tasks().get(1), reviewItem, 1, SubmissionStatus.NEEDS_REVIEW);
        saveSubmission(fixture, codeTask, codeItem, 1, SubmissionStatus.PASSED);
        saveSubmission(fixture, codeTask, codeItem, 2, SubmissionStatus.FAILED);

        mockMvc.perform(
                        get("/api/v1/student/homeworks/{homeworkId}", homework.getId())
                                .with(user(fixture.studentPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(untouchedItem.toString()))
                .andExpect(jsonPath("$.items[0].passed").value(false))
                .andExpect(jsonPath("$.items[0].latestSubmissionStatus").doesNotExist())
                .andExpect(jsonPath("$.items[0].task.codeExecution").doesNotExist())
                .andExpect(jsonPath("$.items[1].passed").value(false))
                .andExpect(jsonPath("$.items[1].latestSubmissionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.items[2].passed").value(true))
                .andExpect(jsonPath("$.items[2].latestSubmissionStatus").value("FAILED"))
                .andExpect(jsonPath("$.items[2].task.codeExecution.language").value("PYTHON"))
                .andExpect(
                        jsonPath("$.items[2].task.codeExecution.starterCode")
                                .value("print('starter')"))
                .andExpect(jsonPath("$.items[2].task.codeExecution.executionEnabled").value(true))
                .andExpect(jsonPath("$.items[2].task.codeExecution.timeLimitMs").value(1200))
                .andExpect(jsonPath("$.items[2].task.codeExecution.memoryLimitMb").value(192))
                .andExpect(jsonPath("$..inputText").doesNotExist())
                .andExpect(jsonPath("$..expectedOutput").doesNotExist());
    }

    @Test
    void foreignHomeworkIsNotDisclosed() throws Exception {
        Fixture owner = createFixture("detail-owner");
        Fixture foreign = createFixture("detail-foreign");
        HomeworkEntity homework =
                createHomework(
                        owner,
                        "Secret",
                        Instant.now(),
                        null,
                        HomeworkStatus.ASSIGNED,
                        null,
                        owner.tasks());

        mockMvc.perform(
                        get("/api/v1/student/homeworks/{homeworkId}", homework.getId())
                                .with(user(foreign.studentPrincipal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOMEWORK_NOT_FOUND"));
    }

    @Test
    void listQueryCountDoesNotGrowWithHomeworkCount() throws Exception {
        Fixture fixture = createFixture("list-performance");
        for (int i = 0; i < 8; i++) {
            createHomework(
                    fixture,
                    "Homework " + i,
                    Instant.now().plusSeconds(i),
                    null,
                    HomeworkStatus.ASSIGNED,
                    null,
                    fixture.tasks());
        }
        Statistics statistics = statistics();
        statistics.clear();

        mockMvc.perform(
                        get("/api/v1/student/homeworks")
                                .with(user(fixture.studentPrincipal()))
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void detailQueryCountDoesNotGrowWithTaskCount() throws Exception {
        Fixture fixture = createFixture("detail-performance");
        List<TaskEntity> tasks = new ArrayList<>(fixture.tasks());
        for (int i = 3; i < 10; i++) {
            tasks.add(createTask(fixture, i));
        }
        HomeworkEntity homework =
                createHomework(
                        fixture,
                        "Large",
                        Instant.now(),
                        null,
                        HomeworkStatus.ASSIGNED,
                        null,
                        tasks);
        Statistics statistics = statistics();
        statistics.clear();

        mockMvc.perform(
                        get("/api/v1/student/homeworks/{homeworkId}", homework.getId())
                                .with(user(fixture.studentPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void openApiPublishesStudentHomeworkOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/v1/student/homeworks'].get.operationId")
                                .value("listStudentHomeworks"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/student/homeworks/{homeworkId}'].get.operationId")
                                .value("getStudentHomework"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkSummaryResponse.properties.id.format")
                                .value("uuid"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkSummaryResponse.properties.assignedAt.format")
                                .value("date-time"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkSummaryResponse.properties.status.enum.length()")
                                .value(3))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkSummaryResponse.properties.completedAt.format")
                                .value("date-time"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentTaskResponse.properties.taskType.enum.length()")
                                .value(5))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentTaskResponse.properties.difficulty.enum.length()")
                                .value(3))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentTaskResponse.properties.codeExecution")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkItemResponse.properties.passed")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentHomeworkItemResponse.properties.latestSubmissionStatus")
                                .exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private Fixture createFixture(String label) {
        UserEntity teacherUser =
                new UserEntity(
                        UUID.randomUUID(),
                        label + "-teacher-" + UUID.randomUUID() + "@example.com",
                        "hash",
                        UserStatus.ACTIVE);
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        TeacherEntity teacher =
                teacherRepository.saveAndFlush(
                        new TeacherEntity(UUID.randomUUID(), teacherUser, "Teacher"));

        UserEntity studentUser =
                new UserEntity(
                        UUID.randomUUID(),
                        label + "-student-" + UUID.randomUUID() + "@example.com",
                        "hash",
                        UserStatus.ACTIVE);
        studentUser.addRole(UserRole.STUDENT);
        userRepository.saveAndFlush(studentUser);
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(
                                UUID.randomUUID(),
                                studentUser.id(),
                                "Student",
                                null,
                                StudentStatus.ACTIVE,
                                null,
                                null));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));

        SubjectEntity subject =
                subjectRepository.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                null,
                                "Subject " + UUID.randomUUID(),
                                null,
                                SubjectStatus.ACTIVE));
        LearningProgramEntity program =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Program",
                                null,
                                LearningProgramStatus.ACTIVE));
        StudentProgramEntity studentProgram =
                studentProgramRepository.saveAndFlush(
                        new StudentProgramEntity(
                                UUID.randomUUID(),
                                student.getId(),
                                program.getId(),
                                teacher.id(),
                                StudentProgramStatus.ACTIVE,
                                480,
                                Instant.now(),
                                null));
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        studentUser.id(),
                        studentUser.email(),
                        "hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        Fixture fixture =
                new Fixture(
                        teacher,
                        student,
                        principal,
                        subject,
                        program,
                        studentProgram,
                        new ArrayList<>());
        fixture.tasks().add(createTask(fixture, 0));
        fixture.tasks().add(createTask(fixture, 1));
        fixture.tasks().add(createTask(fixture, 2));
        return fixture;
    }

    private StudentProgramEntity createStudentProgram(Fixture fixture) {
        LearningProgramEntity program =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                fixture.teacher().id(),
                                fixture.subject().id(),
                                "Other program",
                                null,
                                LearningProgramStatus.ACTIVE));
        return studentProgramRepository.saveAndFlush(
                new StudentProgramEntity(
                        UUID.randomUUID(),
                        fixture.student().getId(),
                        program.getId(),
                        fixture.teacher().id(),
                        StudentProgramStatus.ACTIVE,
                        480,
                        Instant.now(),
                        null));
    }

    private TaskEntity createTask(Fixture fixture, int index) {
        return taskRepository.saveAndFlush(
                new TaskEntity(
                        UUID.randomUUID(),
                        fixture.teacher().id(),
                        fixture.subject().id(),
                        "Task " + index,
                        "Description " + index,
                        TaskType.TEXT,
                        TaskDifficulty.MEDIUM,
                        TaskStatus.ACTIVE));
    }

    private void saveSubmission(
            Fixture fixture,
            TaskEntity task,
            UUID homeworkItemId,
            int attemptNo,
            SubmissionStatus status) {
        submissionRepository.saveAndFlush(
                new SubmissionEntity(
                        UUID.randomUUID(),
                        fixture.student().getId(),
                        fixture.studentProgram().id(),
                        task.getId(),
                        homeworkItemId,
                        attemptNo,
                        status,
                        null,
                        Instant.now()));
    }

    private HomeworkEntity createHomework(
            Fixture fixture,
            StudentProgramEntity studentProgram,
            String title,
            HomeworkStatus status) {
        return createHomework(
                fixture,
                studentProgram,
                title,
                Instant.now(),
                null,
                status,
                null,
                List.of(fixture.tasks().getFirst()));
    }

    private HomeworkEntity createHomework(
            Fixture fixture,
            String title,
            Instant assignedAt,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt,
            List<TaskEntity> tasks) {
        return createHomework(
                fixture,
                fixture.studentProgram(),
                title,
                assignedAt,
                dueAt,
                status,
                completedAt,
                tasks);
    }

    private HomeworkEntity createHomework(
            Fixture fixture,
            StudentProgramEntity studentProgram,
            String title,
            Instant assignedAt,
            Instant dueAt,
            HomeworkStatus status,
            Instant completedAt,
            List<TaskEntity> tasks) {
        UUID homeworkId = UUID.randomUUID();
        List<HomeworkItemEntity> items = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            items.add(
                    new HomeworkItemEntity(
                            UUID.randomUUID(), homeworkId, tasks.get(i).getId(), i, true));
        }
        return homeworkRepository.saveAndFlush(
                new HomeworkEntity(
                        homeworkId,
                        studentProgram.id(),
                        fixture.teacher().id(),
                        title,
                        "Homework description",
                        assignedAt,
                        dueAt,
                        status,
                        completedAt,
                        items));
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private record Fixture(
            TeacherEntity teacher,
            StudentEntity student,
            AuthenticatedUser studentPrincipal,
            SubjectEntity subject,
            LearningProgramEntity program,
            StudentProgramEntity studentProgram,
            List<TaskEntity> tasks) {}
}
