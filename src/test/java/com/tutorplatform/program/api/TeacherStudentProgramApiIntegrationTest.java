package com.tutorplatform.program.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import com.tutorplatform.content.domain.LessonMaterialType;
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
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class TeacherStudentProgramApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(
                registry, "test_teacher_student_program_api", null);
    }

    private static final UUID PYTHON_SUBJECT_ID =
            UUID.fromString("6513554d-dceb-5902-b6df-557c7a94b5c7");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private StudentTopicProgressRepository progressRepository;
    @Autowired private LessonMaterialRepository materialRepository;

    @Test
    void listReturnsAllProgramsInDeterministicOrderWithSubjectAndWithoutStructure()
            throws Exception {
        TeacherContext teacher = createTeacher("program-list@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Алексей");
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        StudentProgramEntity older =
                createProgram(
                        teacher.teacher(),
                        student,
                        "Первая программа",
                        now.minus(2, ChronoUnit.DAYS));
        StudentProgramEntity newer =
                createProgram(
                        teacher.teacher(),
                        student,
                        "Текущая программа",
                        now.minus(1, ChronoUnit.DAYS));

        MvcResult result =
                mockMvc.perform(get(programsUrl(student.getId())).with(user(teacher.principal())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[0].id").value(newer.id().toString()))
                        .andExpect(jsonPath("$[1].id").value(older.id().toString()))
                        .andExpect(jsonPath("$[0].title").value("Текущая программа"))
                        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                        .andExpect(jsonPath("$[0].reportIntervalMinutes").value(480))
                        .andExpect(jsonPath("$[0].subject.id").value(PYTHON_SUBJECT_ID.toString()))
                        .andExpect(jsonPath("$[0].subject.code").value("PYTHON"))
                        .andExpect(jsonPath("$[0].subject.name").value("Python"))
                        .andReturn();

        JsonNode first = json(result).get(0);
        assertThat(first.has("modules")).isFalse();
        assertThat(first.has("topics")).isFalse();
    }

    @Test
    void listReturnsEmptyArrayForOwnedStudentWithoutProgramsAndDoesNotRequireCsrf()
            throws Exception {
        TeacherContext teacher = createTeacher("empty-program-list@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Илья");

        mockMvc.perform(get(programsUrl(student.getId())).with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listNormalizesForeignStudentToNotFound() throws Exception {
        TeacherContext owner = createTeacher("program-owner@example.com");
        TeacherContext foreignTeacher = createTeacher("program-foreign@example.com");
        StudentEntity student = createStudent(owner.teacher(), "Чужой ученик");

        mockMvc.perform(get(programsUrl(student.getId())).with(user(foreignTeacher.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void endpointsRequireAuthenticationAndTeacherRole() throws Exception {
        UUID studentId = UUID.randomUUID();

        mockMvc.perform(get(programsUrl(studentId))).andExpect(status().isUnauthorized());

        AuthenticatedUser studentPrincipal =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        "student@example.com",
                        "password-hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        mockMvc.perform(get(programsUrl(studentId)).with(user(studentPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailReturnsOrderedHierarchyLifecycleAndEveryProgressStatus() throws Exception {
        TeacherContext teacher = createTeacher("program-detail@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Алексей");
        StudentProgramEntity program =
                createProgram(
                        teacher.teacher(),
                        student,
                        "Python с нуля",
                        Instant.now().minus(10, ChronoUnit.DAYS));
        LearningProgramEntity learningProgram =
                learningProgramRepository.findById(program.learningProgramId()).orElseThrow();

        createModule(learningProgram, "Коллекции", 2);
        ModuleEntity firstModule = createModule(learningProgram, "Основы Python", 0);
        createModule(learningProgram, "Управление программой", 1);

        TopicEntity locked = createTopic(firstModule, "Заблокированная", 3, TopicStatus.DRAFT);
        TopicEntity completed = createTopic(firstModule, "Пройденная", 0, TopicStatus.ACTIVE);
        TopicEntity available = createTopic(firstModule, "Доступная", 2, TopicStatus.ARCHIVED);
        TopicEntity inProgress = createTopic(firstModule, "В процессе", 1, TopicStatus.ACTIVE);
        createProgress(program, locked, StudentTopicProgressStatus.LOCKED);
        createProgress(program, completed, StudentTopicProgressStatus.COMPLETED);
        createProgress(program, available, StudentTopicProgressStatus.AVAILABLE);
        createProgress(program, inProgress, StudentTopicProgressStatus.IN_PROGRESS);

        mockMvc.perform(
                        get(programUrl(student.getId(), program.id()))
                                .with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(program.id().toString()))
                .andExpect(
                        jsonPath("$.learningProgramId").value(learningProgram.getId().toString()))
                .andExpect(jsonPath("$.title").value("Python с нуля"))
                .andExpect(jsonPath("$.description").value("Описание Python с нуля"))
                .andExpect(jsonPath("$.subject.code").value("PYTHON"))
                .andExpect(jsonPath("$.modules.length()").value(3))
                .andExpect(jsonPath("$.modules[0].position").value(0))
                .andExpect(jsonPath("$.modules[1].position").value(1))
                .andExpect(jsonPath("$.modules[2].position").value(2))
                .andExpect(jsonPath("$.modules[0].topics.length()").value(4))
                .andExpect(jsonPath("$.modules[0].topics[0].position").value(0))
                .andExpect(jsonPath("$.modules[0].topics[0].topicStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.modules[0].topics[0].progressStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.modules[0].topics[1].progressStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.modules[0].topics[2].topicStatus").value("ARCHIVED"))
                .andExpect(jsonPath("$.modules[0].topics[2].progressStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.modules[0].topics[3].topicStatus").value("DRAFT"))
                .andExpect(jsonPath("$.modules[0].topics[3].progressStatus").value("LOCKED"))
                .andExpect(jsonPath("$.materials").doesNotExist());
    }

    @Test
    void detailReturnsNullWhenTopicProgressRowIsMissing() throws Exception {
        TeacherContext teacher = createTeacher("missing-progress@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Мария");
        StudentProgramEntity program =
                createProgram(teacher.teacher(), student, "Начало", Instant.now());
        LearningProgramEntity learningProgram =
                learningProgramRepository.findById(program.learningProgramId()).orElseThrow();
        ModuleEntity module = createModule(learningProgram, "Модуль", 0);
        createTopic(module, "Без строки прогресса", 0, TopicStatus.ACTIVE);

        mockMvc.perform(
                        get(programUrl(student.getId(), program.id()))
                                .with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.modules[0].topics[0].progressStatus")
                                .value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void detailRejectsProgramFromAnotherStudentAndUnknownProgram() throws Exception {
        TeacherContext teacher = createTeacher("program-path-owner@example.com");
        StudentEntity firstStudent = createStudent(teacher.teacher(), "Первый");
        StudentEntity secondStudent = createStudent(teacher.teacher(), "Второй");
        StudentProgramEntity secondProgram =
                createProgram(teacher.teacher(), secondStudent, "Вторая", Instant.now());

        mockMvc.perform(
                        get(programUrl(firstStudent.getId(), secondProgram.id()))
                                .with(user(teacher.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));

        mockMvc.perform(
                        get(programUrl(firstStudent.getId(), UUID.randomUUID()))
                                .with(user(teacher.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotReadStudentProgram() throws Exception {
        TeacherContext owner = createTeacher("detail-owner@example.com");
        TeacherContext foreignTeacher = createTeacher("detail-foreign@example.com");
        StudentEntity student = createStudent(owner.teacher(), "Чужой ученик");
        StudentProgramEntity program =
                createProgram(owner.teacher(), student, "Скрытая", Instant.now());

        mockMvc.perform(
                        get(programUrl(student.getId(), program.id()))
                                .with(user(foreignTeacher.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void topicIdFromDetailOpensExistingMaterialsApi() throws Exception {
        TeacherContext teacher = createTeacher("material-bridge@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Алексей");
        StudentProgramEntity program =
                createProgram(teacher.teacher(), student, "Материалы", Instant.now());
        LearningProgramEntity learningProgram =
                learningProgramRepository.findById(program.learningProgramId()).orElseThrow();
        ModuleEntity module = createModule(learningProgram, "Модуль", 0);
        TopicEntity topic = createTopic(module, "Тема", 0, TopicStatus.ACTIVE);
        materialRepository.saveAndFlush(
                new LessonMaterialEntity(
                        UUID.randomUUID(),
                        topic.id(),
                        teacher.teacher().id(),
                        LessonMaterialType.TEXT,
                        "Конспект",
                        "Текст материала",
                        null,
                        null,
                        0));

        MvcResult detail =
                mockMvc.perform(
                                get(programUrl(student.getId(), program.id()))
                                        .with(user(teacher.principal())))
                        .andExpect(status().isOk())
                        .andReturn();
        String topicId = json(detail).at("/modules/0/topics/0/id").textValue();

        mockMvc.perform(
                        get("/api/v1/teacher/topics/{topicId}/materials", topicId)
                                .with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Конспект"));
    }

    @Test
    void openApiPublishesProgramOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/programs'].get.operationId")
                                .value("listTeacherStudentPrograms"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/programs/{studentProgramId}'].get.operationId")
                                .value("getTeacherStudentProgram"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/programs/{studentProgramId}'].get.parameters[0].schema.format")
                                .value("uuid"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.StudentProgramDetailsResponse.properties.startedAt.format")
                                .value("date-time"))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.ProgramTopicResponse.properties.topicStatus.enum.length()")
                                .value(3))
                .andExpect(
                        jsonPath(
                                        "$.components.schemas.ProgramTopicResponse.properties.progressStatus.enum.length()")
                                .value(4));
    }

    private TeacherContext createTeacher(String email) {
        UserEntity user =
                new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher =
                teacherRepository.saveAndFlush(
                        new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        user.id(),
                        email,
                        "password-hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        return new TeacherContext(teacher, principal);
    }

    private StudentEntity createStudent(TeacherEntity teacher, String firstName) {
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(
                                UUID.randomUUID(), firstName, null, StudentStatus.ACTIVE));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private StudentProgramEntity createProgram(
            TeacherEntity teacher, StudentEntity student, String title, Instant startedAt) {
        SubjectEntity subject = subjectRepository.findById(PYTHON_SUBJECT_ID).orElseThrow();
        LearningProgramEntity learningProgram =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                title,
                                "Описание " + title,
                                LearningProgramStatus.ACTIVE));
        return studentProgramRepository.saveAndFlush(
                new StudentProgramEntity(
                        UUID.randomUUID(),
                        student.getId(),
                        learningProgram.getId(),
                        teacher.id(),
                        StudentProgramStatus.ACTIVE,
                        480,
                        startedAt,
                        null));
    }

    private ModuleEntity createModule(
            LearningProgramEntity learningProgram, String title, int position) {
        return moduleRepository.saveAndFlush(
                new ModuleEntity(
                        UUID.randomUUID(),
                        learningProgram.getId(),
                        title,
                        "Описание " + title,
                        position));
    }

    private TopicEntity createTopic(
            ModuleEntity module, String title, int position, TopicStatus status) {
        return topicRepository.saveAndFlush(
                new TopicEntity(
                        UUID.randomUUID(),
                        module.id(),
                        title,
                        "Описание " + title,
                        position,
                        status));
    }

    private void createProgress(
            StudentProgramEntity program, TopicEntity topic, StudentTopicProgressStatus status) {
        Instant startedAt =
                status == StudentTopicProgressStatus.IN_PROGRESS
                                || status == StudentTopicProgressStatus.COMPLETED
                        ? Instant.now()
                        : null;
        Instant completedAt = status == StudentTopicProgressStatus.COMPLETED ? Instant.now() : null;
        progressRepository.saveAndFlush(
                new StudentTopicProgressEntity(
                        program.id(), topic.id(), status, startedAt, completedAt));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String programsUrl(UUID studentId) {
        return "/api/v1/teacher/students/" + studentId + "/programs";
    }

    private String programUrl(UUID studentId, UUID studentProgramId) {
        return programsUrl(studentId) + "/" + studentProgramId;
    }

    private record TeacherContext(TeacherEntity teacher, AuthenticatedUser principal) {}
}
