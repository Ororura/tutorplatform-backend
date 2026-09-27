package com.tutorplatform.program.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LearningProgramDuplicationApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(
                registry, "test_learning_program_duplication_api", null);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private LessonMaterialRepository lessonMaterialRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TopicTaskRepository topicTaskRepository;

    @Test
    void duplicateCreatesIndependentDraftStructureAndKeepsSourceAssignment() throws Exception {
        Fixture fixture = createFixture();

        String response =
                mockMvc.perform(
                                post(
                                                "/api/v1/teacher/programs/{programId}/duplicate",
                                                fixture.sourceProgram().getId())
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("DRAFT"))
                        .andExpect(jsonPath("$.title").value("Python — копия"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        UUID copyId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        LearningProgramEntity copy = learningProgramRepository.findById(copyId).orElseThrow();

        assertThat(copy.getTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(copy.getSubjectId()).isEqualTo(fixture.subject().id());
        assertThat(copy.getStatus()).isEqualTo(LearningProgramStatus.DRAFT);
        assertThat(copy.getDescription()).isEqualTo("Исходное описание");

        List<ModuleEntity> copiedModules = moduleRepository.findByLearningProgramId(copyId);
        assertThat(copiedModules).hasSize(1);
        ModuleEntity copiedModule = copiedModules.getFirst();
        assertThat(copiedModule.id()).isNotEqualTo(fixture.sourceModule().id());
        assertThat(copiedModule.title()).isEqualTo(fixture.sourceModule().title());
        assertThat(copiedModule.position()).isEqualTo(fixture.sourceModule().position());

        List<TopicEntity> copiedTopics = topicRepository.findByModuleId(copiedModule.id());
        assertThat(copiedTopics).hasSize(1);
        TopicEntity copiedTopic = copiedTopics.getFirst();
        assertThat(copiedTopic.id()).isNotEqualTo(fixture.sourceTopic().id());
        assertThat(copiedTopic.title()).isEqualTo(fixture.sourceTopic().title());
        assertThat(copiedTopic.status()).isEqualTo(TopicStatus.ACTIVE);

        List<LessonMaterialEntity> copiedMaterials =
                lessonMaterialRepository.findAllByTopicIdOrderByPosition(copiedTopic.id());
        assertThat(copiedMaterials).hasSize(1);
        LessonMaterialEntity copiedMaterial = copiedMaterials.getFirst();
        assertThat(copiedMaterial.getId()).isNotEqualTo(fixture.sourceMaterial().getId());
        assertThat(copiedMaterial.getMaterialType()).isEqualTo(LessonMaterialType.MARKDOWN);
        assertThat(copiedMaterial.getContent()).isEqualTo(fixture.sourceMaterial().getContent());

        List<TopicTaskEntity> copiedTaskLinks =
                topicTaskRepository.findAllByTopicIdOrderByPosition(copiedTopic.id());
        assertThat(copiedTaskLinks).hasSize(1);
        assertThat(copiedTaskLinks.getFirst().taskId()).isEqualTo(fixture.task().getId());
        assertThat(copiedTaskLinks.getFirst().required()).isTrue();

        assertThat(
                        studentProgramRepository.existsByLearningProgramId(
                                fixture.sourceProgram().getId()))
                .isTrue();
        assertThat(studentProgramRepository.existsByLearningProgramId(copyId)).isFalse();
    }

    @Test
    void duplicateRejectsProgramOwnedByAnotherTeacher() throws Exception {
        Fixture owner = createFixture();
        Fixture foreign = createFixture();

        mockMvc.perform(
                        post(
                                        "/api/v1/teacher/programs/{programId}/duplicate",
                                        foreign.sourceProgram().getId())
                                .with(user(owner.principal()))
                                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
    }

    @Test
    void openApiPublishesDuplicateOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/programs/{programId}/duplicate'].post.operationId")
                                .value("duplicateTeacherLearningProgram"));
    }

    private Fixture createFixture() {
        String label = UUID.randomUUID().toString();

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

        SubjectEntity subject =
                subjectRepository.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                null,
                                "Programming",
                                "Subject",
                                SubjectStatus.ACTIVE));

        LearningProgramEntity sourceProgram =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Python",
                                "Исходное описание",
                                LearningProgramStatus.ACTIVE));

        ModuleEntity sourceModule =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(),
                                sourceProgram.getId(),
                                "Основы",
                                "Описание модуля",
                                0));

        TopicEntity sourceTopic =
                topicRepository.saveAndFlush(
                        new TopicEntity(
                                UUID.randomUUID(),
                                sourceModule.id(),
                                "Переменные",
                                "Описание темы",
                                0,
                                TopicStatus.ACTIVE));

        LessonMaterialEntity sourceMaterial =
                lessonMaterialRepository.saveAndFlush(
                        new LessonMaterialEntity(
                                UUID.randomUUID(),
                                sourceTopic.id(),
                                teacher.id(),
                                LessonMaterialType.MARKDOWN,
                                "Конспект",
                                "# Переменные",
                                null,
                                null,
                                0));

        TaskEntity task =
                taskRepository.saveAndFlush(
                        new TaskEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Практика",
                                "Описание задания",
                                TaskType.TEXT,
                                TaskDifficulty.EASY,
                                TaskStatus.ACTIVE));

        topicTaskRepository.saveAndFlush(
                new TopicTaskEntity(sourceTopic.id(), task.getId(), 0, true));

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

        studentProgramRepository.saveAndFlush(
                new StudentProgramEntity(
                        UUID.randomUUID(),
                        student.getId(),
                        sourceProgram.getId(),
                        teacher.id(),
                        StudentProgramStatus.ACTIVE,
                        480,
                        Instant.now(),
                        null));

        AuthenticatedUser principal =
                new AuthenticatedUser(
                        teacherUser.id(),
                        teacherUser.email(),
                        "hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

        return new Fixture(
                teacher,
                subject,
                sourceProgram,
                sourceModule,
                sourceTopic,
                sourceMaterial,
                task,
                principal);
    }

    private record Fixture(
            TeacherEntity teacher,
            SubjectEntity subject,
            LearningProgramEntity sourceProgram,
            ModuleEntity sourceModule,
            TopicEntity sourceTopic,
            LessonMaterialEntity sourceMaterial,
            TaskEntity task,
            AuthenticatedUser principal) {}
}
