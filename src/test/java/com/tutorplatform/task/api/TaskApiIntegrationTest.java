package com.tutorplatform.task.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.task.application.CreateTaskCommand;
import com.tutorplatform.task.application.TaskResult;
import com.tutorplatform.task.application.TaskService;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TopicTaskRepository topicTaskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private TopicRepository topicRepository;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Test
    void teacherCreatesTextTaskWithServerDefaults() throws Exception {
        TaskFixture fixture = createFixture();

        mockMvc.perform(post(tasksUrl())
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.subject().id(), "  Что выведет программа?  ", "Ответ", "EASY")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/teacher/tasks/")))
            .andExpect(jsonPath("$.subjectId").value(fixture.subject().id().toString()))
            .andExpect(jsonPath("$.title").value("Что выведет программа?"))
            .andExpect(jsonPath("$.descriptionMarkdown").value("Ответ"))
            .andExpect(jsonPath("$.taskType").value("TEXT"))
            .andExpect(jsonPath("$.difficulty").value("EASY"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void teacherIdComesFromPrincipal() throws Exception {
        TaskFixture fixture = createFixture();
        MvcResult result = createViaApi(fixture, "Principal owner", "EASY");
        UUID taskId = UUID.fromString(json(result).required("id").textValue());

        assertThat(taskRepository.findById(taskId))
            .get().extracting(TaskEntity::getTeacherId)
            .isEqualTo(fixture.teacher().id());
    }

    @Test
    void invalidTitleIsRejected() throws Exception {
        TaskFixture fixture = createFixture();

        mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.subject().id(), "   ", "Ответ", "EASY")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingOrForeignSubjectIsNotDisclosed() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();

        mockMvc.perform(post(tasksUrl()).with(user(owner.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(UUID.randomUUID(), "Missing", "Ответ", "EASY")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));

        mockMvc.perform(post(tasksUrl()).with(user(owner.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(foreign.subject().id(), "Foreign", "Ответ", "EASY")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));
    }

    @Test
    void publicApiCannotCreateCodeTask() throws Exception {
        TaskFixture fixture = createFixture();
        String request = """
            {
              "subjectId": "%s",
              "title": "Only text",
              "descriptionMarkdown": "Ответ",
              "difficulty": "EASY",
              "taskType": "CODE"
            }
            """.formatted(fixture.subject().id());

        mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskType").value("TEXT"));
    }

    @Test
    void systemSubjectIsAvailableToTeacher() throws Exception {
        TaskFixture fixture = createFixture();
        SubjectEntity systemSubject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), null, "SYSTEM_" + UUID.randomUUID(), "System subject", null,
            SubjectStatus.ACTIVE
        ));

        mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(systemSubject.id(), "System task", "Ответ", "MEDIUM")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subjectId").value(systemSubject.id().toString()));
    }

    @Test
    void teacherGetsOwnTaskAndForeignTaskIsNotDisclosed() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        TaskResult task = createTask(owner, "Owned", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(get(taskUrl(task.id())).with(user(owner.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(task.id().toString()))
            .andExpect(jsonPath("$.title").value("Owned"));

        mockMvc.perform(get(taskUrl(task.id())).with(user(foreign.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void listReturnsOnlyCurrentTeacherTasksWithPagination() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        createTask(owner, "A", TaskDifficulty.EASY, TaskStatus.DRAFT);
        createTask(owner, "B", TaskDifficulty.MEDIUM, TaskStatus.DRAFT);
        createTask(foreign, "Foreign", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(get(tasksUrl()).with(user(owner.principal()))
                .param("page", "0").param("size", "1").param("sort", "title,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("A"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listFiltersBySubjectStatusAndDifficulty() throws Exception {
        TaskFixture fixture = createFixture();
        SubjectEntity secondSubject = createSubject(fixture.teacher());
        createTask(fixture, "Match", TaskDifficulty.HARD, TaskStatus.ACTIVE);
        createTask(fixture, "Wrong difficulty", TaskDifficulty.EASY, TaskStatus.ACTIVE);
        taskService.createTask(fixture.principal(), new CreateTaskCommand(
            secondSubject.id(), "Wrong subject", "Ответ", TaskDifficulty.HARD
        ));

        mockMvc.perform(get(tasksUrl()).with(user(fixture.principal()))
                .param("subjectId", fixture.subject().id().toString())
                .param("status", "ACTIVE")
                .param("difficulty", "HARD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("Match"));
    }

    @Test
    void listSortsByAllowListedFieldAndRejectsInvalidSort() throws Exception {
        TaskFixture fixture = createFixture();
        createTask(fixture, "Zulu", TaskDifficulty.EASY, TaskStatus.DRAFT);
        createTask(fixture, "Alpha", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(get(tasksUrl()).with(user(fixture.principal())).param("sort", "title,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].title").value("Alpha"))
            .andExpect(jsonPath("$.items[1].title").value("Zulu"));

        mockMvc.perform(get(tasksUrl()).with(user(fixture.principal())).param("sort", "teacherId,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("sort"));
    }

    @Test
    void patchUpdatesAllowedFields() throws Exception {
        TaskFixture fixture = createFixture();
        TaskResult task = createTask(fixture, "Before", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(patch(taskUrl(task.id())).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest("After", "Новый текст", "HARD", "ACTIVE", task.version())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("After"))
            .andExpect(jsonPath("$.descriptionMarkdown").value("Новый текст"))
            .andExpect(jsonPath("$.difficulty").value("HARD"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.taskType").value("TEXT"))
            .andExpect(jsonPath("$.subjectId").value(fixture.subject().id().toString()))
            .andExpect(jsonPath("$.version").value(task.version() + 1));
    }

    @Test
    void foreignTaskCannotBeUpdated() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        TaskResult task = createTask(owner, "Owned", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(patch(taskUrl(task.id())).with(user(foreign.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest("Changed", "Text", "MEDIUM", "ACTIVE", task.version())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void optimisticLockingReturnsConflict() throws Exception {
        TaskFixture fixture = createFixture();
        TaskResult task = createTask(fixture, "Versioned", TaskDifficulty.EASY, TaskStatus.DRAFT);
        String first = updateRequest("First", "Text", "MEDIUM", "ACTIVE", task.version());
        String stale = updateRequest("Stale", "Text", "HARD", "ARCHIVED", task.version());

        mockMvc.perform(patch(taskUrl(task.id())).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(first))
            .andExpect(status().isOk());

        mockMvc.perform(patch(taskUrl(task.id())).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(stale))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_VERSION_CONFLICT"));
    }

    @Test
    void taskAttachesToOwnedTopicAndPersistsRequired() throws Exception {
        TaskFixture fixture = createFixture();
        TaskResult task = createTask(fixture, "Attach", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), task.id()))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(0, false)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.topicId").value(fixture.topic().id().toString()))
            .andExpect(jsonPath("$.taskId").value(task.id().toString()))
            .andExpect(jsonPath("$.position").value(0))
            .andExpect(jsonPath("$.required").value(false));

        assertThat(topicTaskRepository.findAllByTopicIdOrderByPosition(fixture.topic().id()))
            .singleElement().satisfies(link -> assertThat(link.required()).isFalse());
    }

    @Test
    void duplicateAttachmentAndPositionAreRejected() throws Exception {
        TaskFixture fixture = createFixture();
        TaskResult first = createTask(fixture, "First", TaskDifficulty.EASY, TaskStatus.DRAFT);
        TaskResult second = createTask(fixture, "Second", TaskDifficulty.EASY, TaskStatus.DRAFT);
        attach(fixture, first.id(), 0, true);

        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), first.id()))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(1, true)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_ALREADY_ATTACHED"));

        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), second.id()))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(0, true)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TASK_TOPIC_POSITION_CONFLICT"));
    }

    @Test
    void negativeAttachmentPositionIsRejected() throws Exception {
        TaskFixture fixture = createFixture();
        TaskResult task = createTask(fixture, "Negative", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), task.id()))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(-1, true)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void foreignTopicAndForeignTaskAreNotDisclosed() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        TaskResult ownerTask = createTask(owner, "Owner", TaskDifficulty.EASY, TaskStatus.DRAFT);
        TaskResult foreignTask = createTask(foreign, "Foreign", TaskDifficulty.EASY, TaskStatus.DRAFT);

        mockMvc.perform(post(attachmentUrl(foreign.topic().id(), ownerTask.id()))
                .with(user(owner.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(0, true)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));

        mockMvc.perform(post(attachmentUrl(owner.topic().id(), foreignTask.id()))
                .with(user(owner.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(0, true)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void crossSubjectAttachmentIsRejected() throws Exception {
        TaskFixture fixture = createFixture();
        SubjectEntity secondSubject = createSubject(fixture.teacher());
        TaskResult task = taskService.createTask(fixture.principal(), new CreateTaskCommand(
            secondSubject.id(), "Other subject", "Text", TaskDifficulty.EASY
        ));

        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), task.id()))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(0, true)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TASK_SUBJECT_MISMATCH"));
    }

    @Test
    void teacherTaskApiEnforcesAuthenticationCsrfAndRole() throws Exception {
        TaskFixture fixture = createFixture();
        AuthenticatedUser student = new AuthenticatedUser(
            UUID.randomUUID(), "student-task@example.com", "password", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get(tasksUrl()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(post(tasksUrl()).with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.subject().id(), "No CSRF", "Text", "EASY")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(get(tasksUrl()).with(user(student)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void openApiPublishesStableTaskOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks'].post.operationId").value("createTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks'].get.operationId").value("listTasks"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}'].get.operationId").value("getTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}'].patch.operationId").value("updateTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/tasks/{taskId}'].post.operationId")
                .value("attachTaskToTopic"))
            .andExpect(jsonPath("$.components.schemas.CreateTaskRequest.properties.taskType").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.id.format").value("uuid"))
            .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.taskType.enum.length()").value(1))
            .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.createdAt.format").value("date-time"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks'].post.responses['400'].content['*/*'].schema.$ref")
                .value("#/components/schemas/ApiError"));
    }

    private TaskFixture createFixture() {
        String email = UUID.randomUUID() + "@example.com";
        UserEntity userEntity = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        userEntity.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(userEntity);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), userEntity, "Teacher"
        ));
        SubjectEntity subject = createSubject(teacher);
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Program", null,
            LearningProgramStatus.DRAFT
        ));
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), program.getId(), "Module", null, 0
        ));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Topic", null, 0, TopicStatus.DRAFT
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            userEntity.id(), email, "password-hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new TaskFixture(principal, teacher, subject, topic);
    }

    private SubjectEntity createSubject(TeacherEntity teacher) {
        return subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Subject " + UUID.randomUUID(), null,
            SubjectStatus.ACTIVE
        ));
    }

    private TaskResult createTask(
        TaskFixture fixture,
        String title,
        TaskDifficulty difficulty,
        TaskStatus desiredStatus
    ) throws Exception {
        TaskResult task = taskService.createTask(fixture.principal(), new CreateTaskCommand(
            fixture.subject().id(), title, "Text", difficulty
        ));
        if (desiredStatus != TaskStatus.DRAFT) {
            mockMvc.perform(patch(taskUrl(task.id())).with(user(fixture.principal())).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest(title, "Text", difficulty.name(), desiredStatus.name(), task.version())))
                .andExpect(status().isOk());
            return taskService.getTask(fixture.principal(), task.id());
        }
        return task;
    }

    private MvcResult createViaApi(TaskFixture fixture, String title, String difficulty) throws Exception {
        return mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.subject().id(), title, "Text", difficulty)))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private void attach(TaskFixture fixture, UUID taskId, int position, boolean required) throws Exception {
        mockMvc.perform(post(attachmentUrl(fixture.topic().id(), taskId))
                .with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(attachRequest(position, required)))
            .andExpect(status().isCreated());
    }

    private String createRequest(
        UUID subjectId,
        String title,
        String descriptionMarkdown,
        String difficulty
    ) throws Exception {
        return objectMapper.writeValueAsString(new CreateBody(
            subjectId, title, descriptionMarkdown, difficulty
        ));
    }

    private String updateRequest(
        String title,
        String descriptionMarkdown,
        String difficulty,
        String status,
        long version
    ) throws Exception {
        return objectMapper.writeValueAsString(new UpdateBody(
            title, descriptionMarkdown, difficulty, status, version
        ));
    }

    private String attachRequest(int position, boolean required) throws Exception {
        return objectMapper.writeValueAsString(new AttachBody(position, required));
    }

    private String tasksUrl() {
        return "/api/v1/teacher/tasks";
    }

    private String taskUrl(UUID taskId) {
        return tasksUrl() + "/" + taskId;
    }

    private String attachmentUrl(UUID topicId, UUID taskId) {
        return "/api/v1/teacher/topics/" + topicId + "/tasks/" + taskId;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record TaskFixture(
        AuthenticatedUser principal,
        TeacherEntity teacher,
        SubjectEntity subject,
        TopicEntity topic
    ) {
    }

    private record CreateBody(
        UUID subjectId,
        String title,
        String descriptionMarkdown,
        String difficulty
    ) {
    }

    private record UpdateBody(
        String title,
        String descriptionMarkdown,
        String difficulty,
        String status,
        long version
    ) {
    }

    private record AttachBody(int position, boolean required) {
    }
}
