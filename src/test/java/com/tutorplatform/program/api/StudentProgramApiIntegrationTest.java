package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.FileAssetEntity;
import com.tutorplatform.content.domain.FileAssetRepository;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.content.domain.StorageProvider;
import com.tutorplatform.file.application.FileStorage;
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
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentProgramApiIntegrationTest extends PostgresIntegrationTest {
    private static final Path STORAGE = temporaryStorage();

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_student_program_api", null);
        registry.add("app.file-storage.directory", () -> STORAGE.toString());
    }

    private static Path temporaryStorage() {
        try {
            return Files.createTempDirectory("student-material-api-");
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void cleanupStorage() throws Exception {
        try (var paths = Files.walk(STORAGE)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    @Autowired
    private MockMvc mockMvc;
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
    private ModuleRepository moduleRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private StudentTopicProgressRepository progressRepository;
    @Autowired
    private LessonMaterialRepository lessonMaterialRepository;
    @Autowired
    private FileAssetRepository fileAssetRepository;
    @Autowired
    private FileStorage fileStorage;

    @Test
    void listReturnsOnlyCurrentStudentsPrograms() throws Exception {
        Fixture owner = createFixture("list-owner");
        Fixture foreign = createFixture("list-foreign");
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        StudentProgramEntity older = createProgram(owner, "Первая программа", now.minus(2, ChronoUnit.DAYS));
        StudentProgramEntity newer = createProgram(owner, "Текущая программа", now.minus(1, ChronoUnit.DAYS));
        createProgram(foreign, "Чужая программа", now);

        mockMvc.perform(get("/api/v1/student/programs").with(user(owner.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(newer.id().toString()))
            .andExpect(jsonPath("$[0].title").value("Текущая программа"))
            .andExpect(jsonPath("$[1].id").value(older.id().toString()))
            .andExpect(jsonPath("$[?(@.title == 'Чужая программа')]").isEmpty())
            .andExpect(jsonPath("$[0].modules").doesNotExist());
    }

    @Test
    void detailReturnsOwnProgramWithOrderedModulesAndTopics() throws Exception {
        Fixture fixture = createFixture("detail");
        StudentProgramEntity program = createProgram(fixture, "Python с нуля", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(program.learningProgramId()).orElseThrow();
        createModule(learningProgram, "Продвинутый модуль", 2);
        ModuleEntity firstModule = createModule(learningProgram, "Основы", 0);
        createModule(learningProgram, "Практика", 1);
        TopicEntity laterTopic = createTopic(firstModule, "Циклы", 2);
        TopicEntity firstTopic = createTopic(firstModule, "Переменные", 0);
        createProgress(program, laterTopic, StudentTopicProgressStatus.LOCKED);
        createProgress(program, firstTopic, StudentTopicProgressStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/v1/student/programs/{studentProgramId}", program.id())
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(program.id().toString()))
            .andExpect(jsonPath("$.title").value("Python с нуля"))
            .andExpect(jsonPath("$.modules.length()").value(3))
            .andExpect(jsonPath("$.modules[0].position").value(0))
            .andExpect(jsonPath("$.modules[1].position").value(1))
            .andExpect(jsonPath("$.modules[2].position").value(2))
            .andExpect(jsonPath("$.modules[0].topics.length()").value(2))
            .andExpect(jsonPath("$.modules[0].topics[0].id").value(firstTopic.id().toString()))
            .andExpect(jsonPath("$.modules[0].topics[0].position").value(0))
            .andExpect(jsonPath("$.modules[0].topics[0].progressStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.modules[0].topics[1].id").value(laterTopic.id().toString()))
            .andExpect(jsonPath("$.modules[0].topics[1].position").value(2))
            .andExpect(jsonPath("$.assignedByTeacherId").doesNotExist())
            .andExpect(jsonPath("$.teacherId").doesNotExist());
    }

    @Test
    void detailDoesNotExposeAnotherStudentsProgram() throws Exception {
        Fixture current = createFixture("access-current");
        Fixture foreign = createFixture("access-foreign");
        StudentProgramEntity foreignProgram = createProgram(foreign, "Чужая программа", Instant.now());

        mockMvc.perform(get("/api/v1/student/programs/{studentProgramId}", foreignProgram.id())
                .with(user(current.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void topicReturnsOwnTopicWithProgressAndOrderedMaterials() throws Exception {
        Fixture fixture = createFixture("topic-detail");
        StudentProgramEntity program = createProgram(fixture, "Java", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(program.learningProgramId()).orElseThrow();
        ModuleEntity module = createModule(learningProgram, "Коллекции", 0);
        TopicEntity topic = createTopic(module, "Списки", 0);
        createProgress(program, topic, StudentTopicProgressStatus.IN_PROGRESS);
        LessonMaterialEntity later = createMaterial(fixture, topic, "Практика", 3);
        LessonMaterialEntity first = createMaterial(fixture, topic, "Теория", 0);

        StudentProgramEntity otherProgram = createProgram(fixture, "Python", Instant.now());
        LearningProgramEntity otherLearningProgram = learningProgramRepository
            .findById(otherProgram.learningProgramId()).orElseThrow();
        TopicEntity otherTopic = createTopic(createModule(otherLearningProgram, "Чужой модуль", 0), "Чужая тема", 0);
        createMaterial(fixture, otherTopic, "Чужой материал", 0);

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}",
                program.id(),
                topic.id()
            ).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(topic.id().toString()))
            .andExpect(jsonPath("$.title").value("Списки"))
            .andExpect(jsonPath("$.description").value("Описание Списки"))
            .andExpect(jsonPath("$.moduleId").value(module.id().toString()))
            .andExpect(jsonPath("$.moduleTitle").value("Коллекции"))
            .andExpect(jsonPath("$.progressStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.materials.length()").value(2))
            .andExpect(jsonPath("$.materials[0].id").value(first.getId().toString()))
            .andExpect(jsonPath("$.materials[0].position").value(0))
            .andExpect(jsonPath("$.materials[1].id").value(later.getId().toString()))
            .andExpect(jsonPath("$.materials[1].position").value(3))
            .andExpect(jsonPath("$.materials[?(@.title == 'Чужой материал')]").isEmpty())
            .andExpect(jsonPath("$.teacherId").doesNotExist())
            .andExpect(jsonPath("$.topicStatus").doesNotExist())
            .andExpect(jsonPath("$.version").doesNotExist())
            .andExpect(jsonPath("$.materials[0].createdByTeacherId").doesNotExist())
            .andExpect(jsonPath("$.materials[0].version").doesNotExist())
            .andExpect(jsonPath("$.materials[0].createdAt").doesNotExist())
            .andExpect(jsonPath("$.materials[0].updatedAt").doesNotExist());
    }

    @Test
    void topicDoesNotExposeAnotherStudentsProgram() throws Exception {
        Fixture current = createFixture("topic-current");
        Fixture foreign = createFixture("topic-foreign");
        StudentProgramEntity foreignProgram = createProgram(foreign, "Чужая программа", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(foreignProgram.learningProgramId()).orElseThrow();
        TopicEntity topic = createTopic(createModule(learningProgram, "Модуль", 0), "Тема", 0);

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}",
                foreignProgram.id(),
                topic.id()
            ).with(user(current.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void topicFromAnotherProgramIsNotFound() throws Exception {
        Fixture fixture = createFixture("topic-other-program");
        StudentProgramEntity requestedProgram = createProgram(fixture, "Первая программа", Instant.now());
        StudentProgramEntity otherProgram = createProgram(fixture, "Вторая программа", Instant.now());
        LearningProgramEntity otherLearningProgram = learningProgramRepository
            .findById(otherProgram.learningProgramId()).orElseThrow();
        TopicEntity otherTopic = createTopic(
            createModule(otherLearningProgram, "Другой модуль", 0),
            "Другая тема",
            0
        );

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}",
                requestedProgram.id(),
                otherTopic.id()
            ).with(user(fixture.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_TOPIC_NOT_FOUND"));
    }

    @Test
    void studentDownloadsFileFromAssignedProgramTopic() throws Exception {
        Fixture fixture = createFixture("download-own");
        StudentProgramEntity program = createProgram(fixture, "Java", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(program.learningProgramId()).orElseThrow();
        TopicEntity topic = createTopic(createModule(learningProgram, "Файлы", 0), "Конспект", 0);
        byte[] content = "student material".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        LessonMaterialEntity material = createFileMaterial(fixture, topic, "lesson.txt", content);

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}/materials/{materialId}/download",
                program.id(), topic.id(), material.getId()
            ).with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(content().bytes(content))
            .andExpect(content().contentType("text/plain"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    }

    @Test
    void studentCannotDownloadFileFromAnotherStudentsProgram() throws Exception {
        Fixture current = createFixture("download-current");
        Fixture foreign = createFixture("download-foreign");
        StudentProgramEntity foreignProgram = createProgram(foreign, "Чужая", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(foreignProgram.learningProgramId()).orElseThrow();
        TopicEntity topic = createTopic(createModule(learningProgram, "Модуль", 0), "Тема", 0);
        LessonMaterialEntity material = createFileMaterial(foreign, topic, "foreign.txt", "secret".getBytes());

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}/materials/{materialId}/download",
                foreignProgram.id(), topic.id(), material.getId()
            ).with(user(current.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void studentCannotDownloadArbitraryTeacherFileAsset() throws Exception {
        Fixture fixture = createFixture("download-arbitrary");
        StudentProgramEntity program = createProgram(fixture, "Java", Instant.now());
        LearningProgramEntity learningProgram = learningProgramRepository
            .findById(program.learningProgramId()).orElseThrow();
        TopicEntity topic = createTopic(createModule(learningProgram, "Модуль", 0), "Тема", 0);
        FileAssetEntity unattachedAsset = createFileAsset(fixture, "private.txt", "private".getBytes());

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}/materials/{materialId}/download",
                program.id(), topic.id(), unattachedAsset.id()
            ).with(user(fixture.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LESSON_MATERIAL_NOT_FOUND"));
    }

    @Test
    void missingTopicIsNotFound() throws Exception {
        Fixture fixture = createFixture("topic-missing");
        StudentProgramEntity program = createProgram(fixture, "Программа", Instant.now());

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}",
                program.id(),
                UUID.randomUUID()
            ).with(user(fixture.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_TOPIC_NOT_FOUND"));
    }

    @Test
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/student/programs"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(get("/api/v1/student/programs/{studentProgramId}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(get(
                "/api/v1/student/programs/{studentProgramId}/topics/{topicId}",
                UUID.randomUUID(),
                UUID.randomUUID()
            ))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void listReturnsEmptyArrayWhenCurrentStudentHasNoPrograms() throws Exception {
        Fixture fixture = createFixture("empty");

        mockMvc.perform(get("/api/v1/student/programs").with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    private Fixture createFixture(String label) {
        UserEntity teacherUser = new UserEntity(
            UUID.randomUUID(), label + "-teacher@example.com", "hash", UserStatus.ACTIVE
        );
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), teacherUser, "Teacher"
        ));

        UserEntity studentUser = new UserEntity(
            UUID.randomUUID(), label + "-student@example.com", "hash", UserStatus.ACTIVE
        );
        studentUser.addRole(UserRole.STUDENT);
        userRepository.saveAndFlush(studentUser);
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), studentUser.id(), "Student", null,
            StudentStatus.ACTIVE, null, null
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));

        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Subject " + label, null, SubjectStatus.ACTIVE
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            studentUser.id(), studentUser.email(), "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        return new Fixture(teacher, student, subject, principal);
    }

    private StudentProgramEntity createProgram(Fixture fixture, String title, Instant startedAt) {
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(
            new LearningProgramEntity(
                UUID.randomUUID(), fixture.teacher().id(), fixture.subject().id(),
                title, "Описание " + title, LearningProgramStatus.ACTIVE
            )
        );
        return studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), fixture.student().getId(), learningProgram.getId(), fixture.teacher().id(),
            StudentProgramStatus.ACTIVE, 480, startedAt, null
        ));
    }

    private ModuleEntity createModule(LearningProgramEntity program, String title, int position) {
        return moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), program.getId(), title, "Описание " + title, position
        ));
    }

    private TopicEntity createTopic(ModuleEntity module, String title, int position) {
        return topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), title, "Описание " + title, position, TopicStatus.ACTIVE
        ));
    }

    private void createProgress(
        StudentProgramEntity program,
        TopicEntity topic,
        StudentTopicProgressStatus status
    ) {
        progressRepository.saveAndFlush(new StudentTopicProgressEntity(
            program.id(), topic.id(), status,
            status == StudentTopicProgressStatus.IN_PROGRESS ? Instant.now() : null,
            null
        ));
    }

    private LessonMaterialEntity createMaterial(
        Fixture fixture,
        TopicEntity topic,
        String title,
        int position
    ) {
        return lessonMaterialRepository.saveAndFlush(new LessonMaterialEntity(
            UUID.randomUUID(),
            topic.id(),
            fixture.teacher().id(),
            LessonMaterialType.TEXT,
            title,
            "Содержимое " + title,
            null,
            null,
            position
        ));
    }

    private LessonMaterialEntity createFileMaterial(
        Fixture fixture,
        TopicEntity topic,
        String filename,
        byte[] content
    ) {
        FileAssetEntity asset = createFileAsset(fixture, filename, content);
        return lessonMaterialRepository.saveAndFlush(new LessonMaterialEntity(
            UUID.randomUUID(), topic.id(), fixture.teacher().id(), LessonMaterialType.FILE,
            filename, null, asset.id(), null, 0
        ));
    }

    private FileAssetEntity createFileAsset(Fixture fixture, String filename, byte[] content) {
        FileStorage.StoredObject object = fileStorage.store(content);
        return fileAssetRepository.saveAndFlush(new FileAssetEntity(
            UUID.randomUUID(), fixture.teacher().id(), StorageProvider.valueOf(object.provider()), object.key(),
            filename, "text/plain", content.length, "test-sha256"
        ));
    }

    private record Fixture(
        TeacherEntity teacher,
        StudentEntity student,
        SubjectEntity subject,
        AuthenticatedUser principal
    ) {
    }
}
