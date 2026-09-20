package com.tutorplatform.task.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
import com.tutorplatform.task.application.ProgrammingTaskConfigInput;
import com.tutorplatform.task.application.TaskTestCaseInput;
import com.tutorplatform.task.application.TaskResult;
import com.tutorplatform.task.application.TaskService;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.programming.*;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_task_api", "008");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

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
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private ProgrammingTaskConfigRepository programmingConfigRepository;
    @Autowired
    private TaskTestCaseRepository testCaseRepository;
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
    void teacherCreatesCodeTaskAtomicallyWithPythonConfigAndTestCases() throws Exception {
        TaskFixture fixture = createFixture();
        MvcResult result = mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(codeCreateRequest(fixture.subject().id(), 5000, 128)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskType").value("CODE"))
            .andExpect(jsonPath("$.programmingConfig.language").value("PYTHON"))
            .andExpect(jsonPath("$.testCases.length()").value(2))
            .andExpect(jsonPath("$.testCases[0].comparisonMode").value("EXACT"))
            .andExpect(jsonPath("$.testCases[0].hidden").value(false))
            .andExpect(jsonPath("$.testCases[1].comparisonMode").value("NORMALIZED"))
            .andExpect(jsonPath("$.testCases[1].hidden").value(true))
            .andReturn();

        UUID taskId = UUID.fromString(json(result).required("id").textValue());
        assertThat(taskRepository.findById(taskId)).get()
            .extracting(TaskEntity::getTaskType).isEqualTo(TaskType.CODE);
        assertThat(programmingConfigRepository.findByTaskId(taskId)).isPresent();
        assertThat(testCaseRepository.findAllByTaskId(taskId)).hasSize(2);
    }

    @Test
    void invalidCodeConfigAndTestCaseRollbackTaskCreation() {
        TaskFixture fixture = createFixture();
        int before = taskRepository.findAllByTeacherId(fixture.teacher().id()).size();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taskService.createTask(
            fixture.principal(), new CreateTaskCommand(
                fixture.subject().id(), "Bad config", "Text", TaskDifficulty.EASY, TaskType.CODE,
                new ProgrammingTaskConfigInput(ProgrammingLanguage.PYTHON, null, true, 99, 128),
                List.of(new TaskTestCaseInput(null, null, "ok", false, ComparisonMode.EXACT, 0))
            )
        )).isInstanceOf(com.tutorplatform.task.application.exception.InvalidProgrammingTaskConfigException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taskService.createTask(
            fixture.principal(), new CreateTaskCommand(
                fixture.subject().id(), "Bad test", "Text", TaskDifficulty.EASY, TaskType.CODE,
                new ProgrammingTaskConfigInput(ProgrammingLanguage.PYTHON, null, true, 5000, 128),
                List.of(new TaskTestCaseInput(null, null, null, false, ComparisonMode.EXACT, 0))
            )
        )).isInstanceOf(com.tutorplatform.task.application.exception.InvalidTaskTestCaseException.class);

        assertThat(taskRepository.findAllByTeacherId(fixture.teacher().id())).hasSize(before);
    }

    @Test
    void codeConfigurationAcceptsBoundaryLimitsAndRejectsOutsideValues() throws Exception {
        TaskFixture fixture = createFixture();
        UUID taskId = createCodeViaApi(fixture, 100, 16);

        mockMvc.perform(put(taskUrl(taskId) + "/programming-config")
                .with(user(fixture.principal())).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(configUpdateRequest(30000, 1024)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timeLimitMs").value(30000))
            .andExpect(jsonPath("$.memoryLimitMb").value(1024));

        for (String body : List.of(configUpdateRequest(99, 128), configUpdateRequest(30001, 128),
            configUpdateRequest(5000, 15), configUpdateRequest(5000, 1025))) {
            mockMvc.perform(put(taskUrl(taskId) + "/programming-config")
                    .with(user(fixture.principal())).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    void codeDetailsExposeNullableInputAndAllHiddenTeacherData() throws Exception {
        TaskFixture fixture = createFixture();
        UUID taskId = createCodeViaApi(fixture, 5000, 128);

        mockMvc.perform(get(taskUrl(taskId)).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.programmingConfig.language").value("PYTHON"))
            .andExpect(jsonPath("$.testCases[0].inputText").doesNotExist())
            .andExpect(jsonPath("$.testCases[1].inputText").value("2 3"))
            .andExpect(jsonPath("$.testCases[1].expectedOutput").value("5"));
    }

    @Test
    void bulkReplacePreservesValidItemsAndIsAtomicOnDuplicatePosition() throws Exception {
        TaskFixture fixture = createFixture();
        UUID taskId = createCodeViaApi(fixture, 5000, 128);
        UUID existingId = testCaseRepository.findAllByTaskId(taskId).getFirst().id();
        String valid = """
            {"items":[
              {"id":"%s","inputText":"7","expectedOutput":"7","hidden":false,"comparisonMode":"NORMALIZED","position":0},
              {"inputText":null,"expectedOutput":"0","hidden":true,"comparisonMode":"EXACT","position":2}
            ]}
            """.formatted(existingId);
        mockMvc.perform(put(taskUrl(taskId) + "/test-cases").with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(valid))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2));

        String duplicate = """
            {"items":[
              {"inputText":"a","expectedOutput":"a","hidden":false,"comparisonMode":"EXACT","position":0},
              {"inputText":"b","expectedOutput":"b","hidden":true,"comparisonMode":"NORMALIZED","position":0}
            ]}
            """;
        mockMvc.perform(put(taskUrl(taskId) + "/test-cases").with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(duplicate))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TASK_TEST_CASE_INVALID"));
        assertThat(testCaseRepository.findAllByTaskId(taskId))
            .extracting(TaskTestCase::expectedOutput).containsExactly("7", "0");
    }

    @Test
    void negativePositionNullOutputAndTextProgrammingOperationsAreRejected() throws Exception {
        TaskFixture fixture = createFixture();
        UUID codeId = createCodeViaApi(fixture, 5000, 128);
        TaskResult text = createTask(fixture, "Text", TaskDifficulty.EASY, TaskStatus.DRAFT);
        String nullOutput = """
            {"items":[{"inputText":null,"expectedOutput":null,"hidden":false,"comparisonMode":"EXACT","position":0}]}
            """;
        String negative = """
            {"items":[{"inputText":null,"expectedOutput":"x","hidden":false,"comparisonMode":"EXACT","position":-1}]}
            """;
        for (String body : List.of(nullOutput, negative)) {
            mockMvc.perform(put(taskUrl(codeId) + "/test-cases").with(user(fixture.principal())).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        mockMvc.perform(put(taskUrl(text.id()) + "/programming-config").with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(configUpdateRequest(5000, 128)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TASK_TYPE_MISMATCH"));
        mockMvc.perform(put(taskUrl(text.id()) + "/test-cases").with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(validSingleTestRequest()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TASK_TYPE_MISMATCH"));
    }

    @Test
    void codeActivationRequiresConfigAndTests() throws Exception {
        TaskFixture fixture = createFixture();
        UUID readyId = createCodeViaApi(fixture, 5000, 128);
        TaskEntity noConfig = taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), fixture.teacher().id(), fixture.subject().id(), "No config", "Text",
            TaskType.CODE, TaskDifficulty.EASY, TaskStatus.DRAFT
        ));
        TaskEntity noTests = taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), fixture.teacher().id(), fixture.subject().id(), "No tests", "Text",
            TaskType.CODE, TaskDifficulty.EASY, TaskStatus.DRAFT
        ));
        programmingConfigRepository.saveAndFlush(new ProgrammingTaskConfig(
            noTests.getId(), ProgrammingLanguage.PYTHON, null, true, 5000, 128
        ));

        TaskResult ready = taskService.getTask(fixture.principal(), readyId);
        mockMvc.perform(patch(taskUrl(readyId)).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(updateRequest("Ready", "Text", "EASY", "ACTIVE", ready.version())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        for (TaskEntity unready : List.of(noConfig, noTests)) {
            mockMvc.perform(patch(taskUrl(unready.getId())).with(user(fixture.principal())).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateRequest(unready.getTitle(), "Text", "EASY", "ACTIVE", unready.getVersion())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TASK_NOT_READY_FOR_ACTIVATION"));
        }
    }

    @Test
    void foreignTeacherCannotReadOrMutateCodeConfigurationOrTests() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        UUID taskId = createCodeViaApi(owner, 5000, 128);
        for (var request : List.of(
            get(taskUrl(taskId)).with(user(foreign.principal())),
            put(taskUrl(taskId) + "/programming-config").with(user(foreign.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(configUpdateRequest(5000, 128)),
            put(taskUrl(taskId) + "/test-cases").with(user(foreign.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(validSingleTestRequest())
        )) {
            mockMvc.perform(request).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
        }
    }

    @Test
    void foreignTestCaseIdCannotBeMovedAcrossTasks() throws Exception {
        TaskFixture fixture = createFixture();
        UUID first = createCodeViaApi(fixture, 5000, 128);
        UUID second = createCodeViaApi(fixture, 5000, 128);
        UUID foreignId = testCaseRepository.findAllByTaskId(second).getFirst().id();
        String body = """
            {"items":[{"id":"%s","inputText":"x","expectedOutput":"x","hidden":false,"comparisonMode":"EXACT","position":0}]}
            """.formatted(foreignId);
        mockMvc.perform(put(taskUrl(first) + "/test-cases").with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TASK_TEST_CASE_INVALID"));
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
    void listsOnlyTasksAttachedToOwnedTopicOrderedByPosition() throws Exception {
        TaskFixture fixture = createFixture();
        TaskFixture other = createFixture();
        TaskResult first = createTask(fixture, "First", TaskDifficulty.EASY, TaskStatus.DRAFT);
        TaskResult second = createTask(fixture, "Second", TaskDifficulty.HARD, TaskStatus.DRAFT);
        TaskResult unrelated = createTask(other, "Unrelated", TaskDifficulty.MEDIUM, TaskStatus.DRAFT);
        attach(fixture, first.id(), 1, false);
        attach(fixture, second.id(), 0, true);
        attach(other, unrelated.id(), 0, true);

        mockMvc.perform(get(topicTasksUrl(fixture.topic().id())).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].taskId").value(second.id().toString()))
            .andExpect(jsonPath("$[0].title").value("Second"))
            .andExpect(jsonPath("$[0].taskType").value("TEXT"))
            .andExpect(jsonPath("$[0].difficulty").value("HARD"))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].position").value(0))
            .andExpect(jsonPath("$[0].required").value(true))
            .andExpect(jsonPath("$[1].taskId").value(first.id().toString()))
            .andExpect(jsonPath("$[1].position").value(1))
            .andExpect(jsonPath("$[1].required").value(false));
    }

    @Test
    void foreignTopicTasksAreNotDisclosed() throws Exception {
        TaskFixture owner = createFixture();
        TaskFixture foreign = createFixture();
        TaskResult task = createTask(owner, "Secret", TaskDifficulty.EASY, TaskStatus.DRAFT);
        attach(owner, task.id(), 0, true);

        mockMvc.perform(get(topicTasksUrl(owner.topic().id())).with(user(foreign.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
    }

    @Test
    void topicTaskListQueryCountDoesNotGrowWithTaskCount() throws Exception {
        TaskFixture fixture = createFixture();
        for (int index = 0; index < 8; index++) {
            TaskResult task = createTask(fixture, "Task " + index, TaskDifficulty.EASY, TaskStatus.DRAFT);
            attach(fixture, task.id(), index, true);
        }
        Statistics statistics = statistics();
        statistics.clear();

        mockMvc.perform(get(topicTasksUrl(fixture.topic().id())).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
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

        UUID codeId = createCodeViaApi(fixture, 5000, 128);
        mockMvc.perform(put(taskUrl(codeId) + "/programming-config").with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON).content(configUpdateRequest(5000, 128)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mockMvc.perform(put(taskUrl(codeId) + "/test-cases").with(user(student)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(validSingleTestRequest()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void openApiPublishesStableTaskOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks'].post.operationId").value("createTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks'].get.operationId").value("listTasks"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}'].get.operationId").value("getTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}'].patch.operationId").value("updateTask"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}/programming-config'].put.operationId")
                .value("updateProgrammingTaskConfig"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/tasks/{taskId}/test-cases'].put.operationId")
                .value("replaceTaskTestCases"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/tasks'].get.operationId")
                .value("listTopicTasks"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/tasks/{taskId}'].post.operationId")
                .value("attachTaskToTopic"))
            .andExpect(jsonPath("$.components.schemas.CreateTaskRequest.properties.taskType").exists())
            .andExpect(jsonPath("$.components.schemas.ProgrammingLanguage.enum[0]").value("PYTHON"))
            .andExpect(jsonPath("$.components.schemas.ComparisonMode.enum.length()").value(2))
            .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.id.format").value("uuid"))
            .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.taskType.enum.length()").value(2))
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

    private UUID createCodeViaApi(TaskFixture fixture, int timeLimitMs, int memoryLimitMb) throws Exception {
        MvcResult result = mockMvc.perform(post(tasksUrl()).with(user(fixture.principal())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeCreateRequest(fixture.subject().id(), timeLimitMs, memoryLimitMb)))
            .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(json(result).required("id").textValue());
    }

    private String codeCreateRequest(UUID subjectId, int timeLimitMs, int memoryLimitMb) {
        return """
            {
              "subjectId":"%s","taskType":"CODE","title":"Сумма двух чисел",
              "descriptionMarkdown":"Считайте два числа.","difficulty":"EASY",
              "programmingConfig":{"language":"PYTHON","starterCode":"print()","executionEnabled":true,
                "timeLimitMs":%d,"memoryLimitMb":%d},
              "testCases":[
                {"inputText":null,"expectedOutput":"","hidden":false,"comparisonMode":"EXACT","position":0},
                {"inputText":"2 3","expectedOutput":"5","hidden":true,"comparisonMode":"NORMALIZED","position":1}
              ]
            }
            """.formatted(subjectId, timeLimitMs, memoryLimitMb);
    }

    private String configUpdateRequest(int timeLimitMs, int memoryLimitMb) {
        return """
            {"starterCode":"updated","executionEnabled":true,"timeLimitMs":%d,"memoryLimitMb":%d}
            """.formatted(timeLimitMs, memoryLimitMb);
    }

    private String validSingleTestRequest() {
        return """
            {"items":[{"inputText":null,"expectedOutput":"ok","hidden":false,"comparisonMode":"EXACT","position":0}]}
            """;
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

    private String topicTasksUrl(UUID topicId) {
        return "/api/v1/teacher/topics/" + topicId + "/tasks";
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
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
