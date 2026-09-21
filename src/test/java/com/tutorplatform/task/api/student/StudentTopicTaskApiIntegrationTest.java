package com.tutorplatform.task.api.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
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
import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfigRepository;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.programming.TaskTestCaseRepository;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentTopicTaskApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_student_topic_task_api", null);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TopicTaskRepository topicTaskRepository;
    @Autowired private ProgrammingTaskConfigRepository programmingConfigRepository;
    @Autowired private TaskTestCaseRepository testCaseRepository;

    @Test
    void returnsOnlyActiveTextAndCodeTasksInTopicOrder() throws Exception {
        Fixture fixture = createFixture("available");
        TaskEntity textTask =
                createTask(fixture, "Текстовая практика", TaskType.TEXT, TaskStatus.ACTIVE);
        TaskEntity draftTask = createTask(fixture, "Черновик", TaskType.TEXT, TaskStatus.DRAFT);
        TaskEntity archivedTask = createTask(fixture, "Архив", TaskType.TEXT, TaskStatus.ARCHIVED);
        TaskEntity codeTask = createCodeTask(fixture, "Практика с кодом");
        attach(fixture.topic(), codeTask, 3, false);
        attach(fixture.topic(), draftTask, 1, true);
        attach(fixture.topic(), textTask, 0, true);
        attach(fixture.topic(), archivedTask, 2, true);

        mockMvc.perform(
                        get(tasksUrl(fixture.program(), fixture.topic()))
                                .with(user(fixture.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(textTask.getId().toString()))
                .andExpect(jsonPath("$[0].taskType").value("TEXT"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[0].required").value(true))
                .andExpect(jsonPath("$[0].programmingConfig").doesNotExist())
                .andExpect(jsonPath("$[0].testCases").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(codeTask.getId().toString()))
                .andExpect(jsonPath("$[1].taskType").value("CODE"))
                .andExpect(jsonPath("$[1].position").value(3))
                .andExpect(jsonPath("$[?(@.title == 'Черновик')]").isEmpty())
                .andExpect(jsonPath("$[?(@.title == 'Архив')]").isEmpty());
    }

    @Test
    void codeTaskExposesPublicConfigurationAndTestsWithoutHiddenTestData() throws Exception {
        Fixture fixture = createFixture("hidden");
        TaskEntity codeTask = createCodeTask(fixture, "Сумма чисел");
        UUID publicTestId = UUID.randomUUID();
        UUID hiddenTestId = UUID.randomUUID();
        testCaseRepository.saveAllAndFlush(
                List.of(
                        new TaskTestCase(
                                publicTestId,
                                codeTask.getId(),
                                "2 3",
                                "5",
                                false,
                                ComparisonMode.NORMALIZED,
                                0),
                        new TaskTestCase(
                                hiddenTestId,
                                codeTask.getId(),
                                "hidden input",
                                "DO_NOT_LEAK_EXPECTED_OUTPUT",
                                true,
                                ComparisonMode.EXACT,
                                1)));
        attach(fixture.topic(), codeTask, 0, true);

        MvcResult result =
                mockMvc.perform(
                                get(tasksUrl(fixture.program(), fixture.topic()))
                                        .with(user(fixture.principal())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].programmingConfig.language").value("PYTHON"))
                        .andExpect(
                                jsonPath("$[0].programmingConfig.starterCode")
                                        .value("print('starter')"))
                        .andExpect(jsonPath("$[0].programmingConfig.executionEnabled").value(true))
                        .andExpect(jsonPath("$[0].programmingConfig.timeLimitMs").value(5000))
                        .andExpect(jsonPath("$[0].programmingConfig.memoryLimitMb").value(128))
                        .andExpect(jsonPath("$[0].programmingConfig.createdAt").doesNotExist())
                        .andExpect(jsonPath("$[0].programmingConfig.updatedAt").doesNotExist())
                        .andExpect(jsonPath("$[0].testCases.length()").value(1))
                        .andExpect(jsonPath("$[0].testCases[0].id").value(publicTestId.toString()))
                        .andExpect(jsonPath("$[0].testCases[0].inputText").value("2 3"))
                        .andExpect(jsonPath("$[0].testCases[0].expectedOutput").value("5"))
                        .andExpect(jsonPath("$[0].testCases[0].comparisonMode").value("NORMALIZED"))
                        .andExpect(jsonPath("$[0].testCases[0].hidden").doesNotExist())
                        .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(hiddenTestId.toString())
                .doesNotContain("hidden input")
                .doesNotContain("DO_NOT_LEAK_EXPECTED_OUTPUT");
    }

    @Test
    void doesNotExposeForeignProgramOrTopicFromAnotherProgram() throws Exception {
        Fixture current = createFixture("owner-current");
        Fixture foreign = createFixture("owner-foreign");

        mockMvc.perform(
                        get(tasksUrl(foreign.program(), foreign.topic()))
                                .with(user(current.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));

        Fixture anotherCurrentProgram = createProgramFixture(current, "owner-current-other");
        mockMvc.perform(
                        get(tasksUrl(current.program(), anotherCurrentProgram.topic()))
                                .with(user(current.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_TOPIC_NOT_FOUND"));
    }

    private Fixture createFixture(String label) {
        UserEntity teacherUser =
                new UserEntity(
                        UUID.randomUUID(),
                        label + "-teacher@example.com",
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
                        label + "-student@example.com",
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
                                "Subject " + label,
                                null,
                                SubjectStatus.ACTIVE));
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        studentUser.id(),
                        studentUser.email(),
                        "hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        return createProgramFixture(
                new Fixture(teacher, student, subject, null, null, principal), label);
    }

    private Fixture createProgramFixture(Fixture fixture, String label) {
        LearningProgramEntity learningProgram =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                fixture.teacher().id(),
                                fixture.subject().id(),
                                "Program " + label,
                                "Description",
                                LearningProgramStatus.ACTIVE));
        StudentProgramEntity program =
                studentProgramRepository.saveAndFlush(
                        new StudentProgramEntity(
                                UUID.randomUUID(),
                                fixture.student().getId(),
                                learningProgram.getId(),
                                fixture.teacher().id(),
                                StudentProgramStatus.ACTIVE,
                                480,
                                Instant.now(),
                                null));
        ModuleEntity module =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(),
                                learningProgram.getId(),
                                "Module",
                                "Description",
                                0));
        TopicEntity topic =
                topicRepository.saveAndFlush(
                        new TopicEntity(
                                UUID.randomUUID(),
                                module.id(),
                                "Topic",
                                "Description",
                                0,
                                TopicStatus.ACTIVE));
        return new Fixture(
                fixture.teacher(),
                fixture.student(),
                fixture.subject(),
                program,
                topic,
                fixture.principal());
    }

    private TaskEntity createTask(Fixture fixture, String title, TaskType type, TaskStatus status) {
        return taskRepository.saveAndFlush(
                new TaskEntity(
                        UUID.randomUUID(),
                        fixture.teacher().id(),
                        fixture.subject().id(),
                        title,
                        "Описание задания",
                        type,
                        TaskDifficulty.EASY,
                        status));
    }

    private TaskEntity createCodeTask(Fixture fixture, String title) {
        TaskEntity task = createTask(fixture, title, TaskType.CODE, TaskStatus.ACTIVE);
        programmingConfigRepository.saveAndFlush(
                new ProgrammingTaskConfig(
                        task.getId(),
                        ProgrammingLanguage.PYTHON,
                        "print('starter')",
                        true,
                        5000,
                        128));
        return task;
    }

    private void attach(TopicEntity topic, TaskEntity task, int position, boolean required) {
        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(topic.id(), task.getId(), position, required));
    }

    private String tasksUrl(StudentProgramEntity program, TopicEntity topic) {
        return "/api/v1/student/programs/%s/topics/%s/tasks".formatted(program.id(), topic.id());
    }

    private record Fixture(
            TeacherEntity teacher,
            StudentEntity student,
            SubjectEntity subject,
            StudentProgramEntity program,
            TopicEntity topic,
            AuthenticatedUser principal) {}
}
