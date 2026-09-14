package com.tutorplatform.program.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
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
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class TeacherProgramManagementApiIntegrationTest {
    private static final UUID PYTHON = UUID.fromString("6513554d-dceb-5902-b6df-557c7a94b5c7");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherStudentLinkRepository linkRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired LearningProgramRepository learningProgramRepository;
    @Autowired StudentProgramRepository studentProgramRepository;
    @Autowired ModuleRepository moduleRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired StudentTopicProgressRepository progressRepository;

    @Test
    void subjectsReturnSystemAndOwnButNotForeignAndDefaultToActive() throws Exception {
        TeacherContext teacher = teacher("subjects-owner@example.com");
        TeacherContext foreign = teacher("subjects-foreign@example.com");
        SubjectEntity own = subject(teacher.teacher.id(), "Own", SubjectStatus.ACTIVE);
        subject(foreign.teacher.id(), "Foreign", SubjectStatus.ACTIVE);
        SubjectEntity archived = subject(teacher.teacher.id(), "Archived", SubjectStatus.ARCHIVED);

        mockMvc.perform(get("/api/v1/teacher/subjects").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '%s')]", PYTHON).exists())
            .andExpect(jsonPath("$[?(@.id == '%s')]", own.id()).exists())
            .andExpect(jsonPath("$[?(@.name == 'Foreign')]").isEmpty())
            .andExpect(jsonPath("$[?(@.id == '%s')]", archived.id()).isEmpty());

        mockMvc.perform(get("/api/v1/teacher/subjects?status=ARCHIVED").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(archived.id().toString()));
    }

    @Test
    void subjectEndpointRequiresTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/subjects")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/teacher/subjects").with(user(studentPrincipal())))
            .andExpect(status().isForbidden());
    }

    @Test
    void createListAndActivateOwnTemplateWithAuthorizedSubjects() throws Exception {
        TeacherContext teacher = teacher("program-create@example.com");
        TeacherContext foreign = teacher("program-create-foreign@example.com");
        SubjectEntity own = subject(teacher.teacher.id(), "Teacher subject", SubjectStatus.ACTIVE);
        SubjectEntity foreignSubject = subject(foreign.teacher.id(), "Foreign subject", SubjectStatus.ACTIVE);
        program(foreign.teacher, "Foreign template", LearningProgramStatus.DRAFT);

        String body = objectMapper.writeValueAsString(new CreateLearningProgramRequest(
            own.id(), "  Python с нуля  ", "Описание"
        ));
        String response = mockMvc.perform(post("/api/v1/teacher/programs")
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Python с нуля"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.subject.id").value(own.id().toString()))
            .andReturn().getResponse().getContentAsString();
        UUID programId = UUID.fromString(objectMapper.readTree(response).get("id").textValue());
        assertThat(learningProgramRepository.findById(programId).orElseThrow().getTeacherId())
            .isEqualTo(teacher.teacher.id());

        mockMvc.perform(get("/api/v1/teacher/programs?status=DRAFT").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(programId.toString()))
            .andExpect(jsonPath("$[0].modules").doesNotExist())
            .andExpect(jsonPath("$[?(@.title == 'Foreign template')]").isEmpty());

        mockMvc.perform(post("/api/v1/teacher/programs").with(user(teacher.principal)).with(csrf())
                .contentType("application/json").content(objectMapper.writeValueAsString(
                    new CreateLearningProgramRequest(PYTHON, "System Python", null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subject.code").value("PYTHON"));

        mockMvc.perform(post("/api/v1/teacher/programs/{id}/activate", programId)
                .with(user(teacher.principal)).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/teacher/programs").with(user(teacher.principal)).with(csrf())
                .contentType("application/json").content(objectMapper.writeValueAsString(
                    new CreateLearningProgramRequest(foreignSubject.id(), "Denied", null))))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));
    }

    @Test
    void assignmentIsOwnedActiveAtomicVisibleAndInitializesLockedProgress() throws Exception {
        TeacherContext teacher = teacher("assign@example.com");
        StudentEntity student = student(teacher.teacher, "Илья");
        LearningProgramEntity program = program(teacher.teacher, "Python template", LearningProgramStatus.ACTIVE);
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), program.getId(), "M", null, 0));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(UUID.randomUUID(), module.id(), "T", null, 0, TopicStatus.ACTIVE));

        String body = "{\"learningProgramId\":\"" + program.getId() + "\"}";
        String response = mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", student.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.reportIntervalMinutes").value(480))
            .andExpect(jsonPath("$.title").value("Python template"))
            .andReturn().getResponse().getContentAsString();
        UUID studentProgramId = UUID.fromString(objectMapper.readTree(response).get("id").textValue());

        var assigned = studentProgramRepository.findById(studentProgramId).orElseThrow();
        assertThat(assigned.studentId()).isEqualTo(student.getId());
        assertThat(assigned.assignedByTeacherId()).isEqualTo(teacher.teacher.id());
        assertThat(progressRepository.findById(studentProgramId, topic.id()).orElseThrow().status())
            .isEqualTo(StudentTopicProgressStatus.LOCKED);

        mockMvc.perform(get("/api/v1/teacher/students/{id}/programs", student.getId()).with(user(teacher.principal)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(studentProgramId.toString()));
        mockMvc.perform(get("/api/v1/teacher/students/{studentId}/programs/{programId}", student.getId(), studentProgramId)
                .with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules[0].topics[0].progressStatus").value("LOCKED"));

        mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", student.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_ALREADY_ASSIGNED"));
    }

    @Test
    void assignmentRejectsForeignResourcesDraftArchivedAndInvalidIntervalButAllowsDifferentProgram() throws Exception {
        TeacherContext teacher = teacher("assign-owner@example.com");
        TeacherContext foreign = teacher("assign-foreign@example.com");
        StudentEntity ownStudent = student(teacher.teacher, "Own");
        StudentEntity foreignStudent = student(foreign.teacher, "Foreign");
        LearningProgramEntity active = program(teacher.teacher, "Active", LearningProgramStatus.ACTIVE);
        LearningProgramEntity second = program(teacher.teacher, "Second", LearningProgramStatus.ACTIVE);
        LearningProgramEntity draft = program(teacher.teacher, "Draft", LearningProgramStatus.DRAFT);
        LearningProgramEntity archived = program(teacher.teacher, "Archived", LearningProgramStatus.ARCHIVED);
        LearningProgramEntity foreignProgram = program(foreign.teacher, "Foreign", LearningProgramStatus.ACTIVE);

        expectAssign(teacher, foreignStudent.getId(), active.getId(), null, 404, "STUDENT_NOT_FOUND");
        expectAssign(teacher, ownStudent.getId(), foreignProgram.getId(), null, 404, "LEARNING_PROGRAM_NOT_FOUND");
        expectAssign(teacher, ownStudent.getId(), draft.getId(), null, 409, "LEARNING_PROGRAM_STATUS_CONFLICT");
        expectAssign(teacher, ownStudent.getId(), archived.getId(), null, 409, "LEARNING_PROGRAM_STATUS_CONFLICT");
        expectAssign(teacher, ownStudent.getId(), active.getId(), 0, 400, "VALIDATION_ERROR");
        expectAssign(teacher, ownStudent.getId(), active.getId(), 120, 201, null);
        expectAssign(teacher, ownStudent.getId(), second.getId(), 240, 201, null);
    }

    @Test
    void openApiPublishesStableManagementOperationIds() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/subjects'].get.operationId").value("listTeacherSubjects"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs'].get.operationId").value("listTeacherLearningPrograms"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs'].post.operationId").value("createTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/activate'].post.operationId").value("activateTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/programs'].post.operationId").value("assignTeacherStudentProgram"));
    }

    private void expectAssign(TeacherContext teacher, UUID studentId, UUID programId, Integer interval,
                              int expectedStatus, String code) throws Exception {
        String intervalJson = interval == null ? "" : ",\"reportIntervalMinutes\":" + interval;
        var action = mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", studentId)
            .with(user(teacher.principal)).with(csrf()).contentType("application/json")
            .content("{\"learningProgramId\":\"" + programId + "\"" + intervalJson + "}"));
        action.andExpect(status().is(expectedStatus));
        if (code != null) action.andExpect(jsonPath("$.code").value(code));
    }

    private TeacherContext teacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        return new TeacherContext(teacher, new AuthenticatedUser(user.id(), email, "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
    }

    private AuthenticatedUser studentPrincipal() {
        return new AuthenticatedUser(UUID.randomUUID(), "student@example.com", "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private StudentEntity student(TeacherEntity teacher, String name) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(UUID.randomUUID(), name, null, StudentStatus.ACTIVE));
        linkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private SubjectEntity subject(UUID ownerId, String name, SubjectStatus status) {
        return subjectRepository.saveAndFlush(new SubjectEntity(UUID.randomUUID(), ownerId, null, name, null, status));
    }

    private LearningProgramEntity program(TeacherEntity teacher, String title, LearningProgramStatus status) {
        return learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), PYTHON, title, null, status
        ));
    }

    private record TeacherContext(TeacherEntity teacher, AuthenticatedUser principal) {
    }
}
