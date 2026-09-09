package com.tutorplatform.homework.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.application.CreateHomeworkCommand;
import com.tutorplatform.homework.application.HomeworkItemInput;
import com.tutorplatform.homework.application.HomeworkResult;
import com.tutorplatform.homework.application.HomeworkService;
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
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.user.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HomeworkApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HomeworkService homeworkService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private StudentProgramRepository studentProgramRepository;
    @Autowired
    private TaskRepository taskRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Test
    void postCreatesHomeworkAndReturnsDetails() throws Exception {
        Fixture fixture = createFixture("api-create-homework@example.com");

        mockMvc.perform(post(homeworksUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/homeworks/")))
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgram().id().toString()))
            .andExpect(jsonPath("$.title").value("Домашнее задание №1"))
            .andExpect(jsonPath("$.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.assignedAt").isNotEmpty())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].taskTitle").value("Первая задача"))
            .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void getReturnsDetailsAndListReturnsSummaryPage() throws Exception {
        Fixture fixture = createFixture("api-read-homework@example.com");
        HomeworkResult homework = createHomework(fixture);

        mockMvc.perform(get(homeworkUrl(fixture.student().getId(), homework.id()))
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(homework.id().toString()))
            .andExpect(jsonPath("$.description").value("Описание"))
            .andExpect(jsonPath("$.items[0].position").value(0))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get(homeworksUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .param("page", "0")
                .param("size", "20")
                .param("sort", "assignedAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(homework.id().toString()))
            .andExpect(jsonPath("$.items[0].description").doesNotExist())
            .andExpect(jsonPath("$.items[0].overdue").value(false))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void patchUpdatesHomeworkWithoutAcceptingAssignmentContext() throws Exception {
        Fixture fixture = createFixture("api-update-homework@example.com");
        HomeworkResult homework = createHomework(fixture);

        mockMvc.perform(patch(homeworkUrl(fixture.student().getId(), homework.id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(fixture, homework.version())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Обновлённое ДЗ"))
            .andExpect(jsonPath("$.description").value("Новое описание"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].taskId").value(fixture.secondTask().getId().toString()))
            .andExpect(jsonPath("$.version").value(homework.version() + 1));
    }

    @Test
    void cancelEndpointOnlyChangesLifecycleStatus() throws Exception {
        Fixture fixture = createFixture("api-cancel-homework@example.com");
        HomeworkResult homework = createHomework(fixture);

        mockMvc.perform(post(homeworkUrl(fixture.student().getId(), homework.id()) + "/cancel")
                .with(user(fixture.principal()))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void arbitrarySortIsRejected() throws Exception {
        Fixture fixture = createFixture("api-sort-homework@example.com");

        mockMvc.perform(get(homeworksUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .param("sort", "assignedByTeacherId,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("sort"));
    }

    @Test
    void foreignStudentAndHomeworkAreNotDisclosed() throws Exception {
        Fixture owner = createFixture("api-homework-owner@example.com");
        Fixture foreign = createFixture("api-homework-foreign@example.com");
        HomeworkResult homework = createHomework(owner);

        mockMvc.perform(get(homeworkUrl(owner.student().getId(), homework.id()))
                .with(user(foreign.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(homeworksUrl(UUID.randomUUID())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        Fixture fixture = createFixture("api-homework-post-csrf@example.com");

        mockMvc.perform(post(homeworksUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture)))
            .andExpect(status().isForbidden());
    }

    @Test
    void patchWithoutCsrfIsForbidden() throws Exception {
        Fixture fixture = createFixture("api-homework-patch-csrf@example.com");
        HomeworkResult homework = createHomework(fixture);

        mockMvc.perform(patch(homeworkUrl(fixture.student().getId(), homework.id()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(fixture, homework.version())))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentRoleCannotAccessTeacherHomeworkApi() throws Exception {
        AuthenticatedUser studentPrincipal = new AuthenticatedUser(
            UUID.randomUUID(), "student-homework@example.com", "password-hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get(homeworksUrl(UUID.randomUUID())).with(user(studentPrincipal)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void requestValidationUsesExistingApiErrorFormat() throws Exception {
        Fixture fixture = createFixture("api-homework-validation@example.com");
        String request = """
            {
              "studentProgramId": "%s",
              "title": " ",
              "items": []
            }
            """.formatted(fixture.studentProgram().id());

        mockMvc.perform(post(homeworksUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void openApiPublishesHomeworkOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/homeworks'].post.operationId")
                .value("createHomework"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/homeworks'].get.operationId")
                .value("listHomeworks"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/homeworks/{homeworkId}'].get.operationId")
                .value("getHomework"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/homeworks/{homeworkId}'].patch.operationId")
                .value("updateHomework"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/homeworks/{homeworkId}/cancel'].post.operationId")
                .value("cancelHomework"))
            .andExpect(jsonPath("$.components.schemas.CreateHomeworkRequest.properties.studentProgramId.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.CreateHomeworkRequest.properties.dueAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.components.schemas.HomeworkDetailsResponse.properties.status.enum.length()")
                .value(3))
            .andExpect(jsonPath("$.components.schemas.ApiError.properties.code").exists());
    }

    private HomeworkResult createHomework(Fixture fixture) {
        return homeworkService.createHomework(
            fixture.principal(),
            new CreateHomeworkCommand(
                fixture.student().getId(), fixture.studentProgram().id(),
                "Домашнее задание №1", "Описание", Instant.now().plusSeconds(3600),
                List.of(
                    new HomeworkItemInput(fixture.firstTask().getId(), 0, true),
                    new HomeworkItemInput(fixture.secondTask().getId(), 1, true)
                )
            )
        );
    }

    private String createRequest(Fixture fixture) {
        return """
            {
              "studentProgramId": "%s",
              "title": "Домашнее задание №1",
              "description": "Описание",
              "dueAt": "2026-09-15T18:00:00Z",
              "items": [
                {"taskId": "%s", "position": 0, "required": true},
                {"taskId": "%s", "position": 1, "required": true}
              ]
            }
            """.formatted(
            fixture.studentProgram().id(),
            fixture.firstTask().getId(),
            fixture.secondTask().getId()
        );
    }

    private String updateRequest(Fixture fixture, long version) {
        return """
            {
              "title": "Обновлённое ДЗ",
              "description": "Новое описание",
              "dueAt": "2026-09-16T18:00:00Z",
              "version": %d,
              "items": [
                {"taskId": "%s", "position": 0, "required": false}
              ]
            }
            """.formatted(version, fixture.secondTask().getId());
    }

    private Fixture createFixture(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), user, "Teacher"
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(), email, "password-hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), "Ученик", null, StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Предмет " + UUID.randomUUID(), null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Программа", null,
            LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        TaskEntity first = createTask(teacher, subject, "Первая задача");
        TaskEntity second = createTask(teacher, subject, "Вторая задача");
        return new Fixture(teacher, principal, student, studentProgram, first, second);
    }

    private TaskEntity createTask(TeacherEntity teacher, SubjectEntity subject, String title) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), title, "Условие",
            TaskType.TEXT, TaskDifficulty.EASY, TaskStatus.ACTIVE
        ));
    }

    private String homeworksUrl(UUID studentId) {
        return "/api/v1/teacher/students/" + studentId + "/homeworks";
    }

    private String homeworkUrl(UUID studentId, UUID homeworkId) {
        return homeworksUrl(studentId) + "/" + homeworkId;
    }

    private record Fixture(
        TeacherEntity teacher,
        AuthenticatedUser principal,
        StudentEntity student,
        StudentProgramEntity studentProgram,
        TaskEntity firstTask,
        TaskEntity secondTask
    ) {
    }
}
