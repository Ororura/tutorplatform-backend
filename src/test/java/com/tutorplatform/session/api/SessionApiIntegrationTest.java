package com.tutorplatform.session.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.session.application.CreateLessonSessionCommand;
import com.tutorplatform.session.application.LessonSessionResult;
import com.tutorplatform.session.application.LessonSessionService;
import com.tutorplatform.session.application.LessonSessionTopicInput;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.user.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.time.Instant;
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
class SessionApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LessonSessionService lessonSessionService;
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

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "008");
    }

    @Test
    void postCreatesLessonSession() throws Exception {
        SessionFixture fixture = createFixture("api-create-session@example.com");

        MvcResult result = mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.studentProgram().id(), fixture.topic().id(), 60)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/sessions/")))
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgram().id().toString()))
            .andExpect(jsonPath("$.durationMinutes").value(60))
            .andExpect(jsonPath("$.attendanceStatus").value("ATTENDED"))
            .andExpect(jsonPath("$.summary").value("Разобрали циклы"))
            .andExpect(jsonPath("$.privateNotes").value("Повторить вложенные циклы"))
            .andExpect(jsonPath("$.topics[0].topicId").value(fixture.topic().id().toString()))
            .andExpect(jsonPath("$.topics[0].isPrimary").value(true))
            .andExpect(jsonPath("$.version").value(0))
            .andReturn();

        assertThat(json(result).required("id").textValue()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(AttendanceStatus.class)
    void postCreatesEveryAttendanceStatus(AttendanceStatus statusValue) throws Exception {
        SessionFixture fixture = createFixture("api-status-" + statusValue.name().toLowerCase() + "@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    fixture.studentProgram().id(),
                    fixture.topic().id(),
                    60,
                    statusValue
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.attendanceStatus").value(statusValue.name()));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 600})
    void postAcceptsDurationBoundaries(int durationMinutes) throws Exception {
        SessionFixture fixture = createFixture("api-duration-" + durationMinutes + "@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    fixture.studentProgram().id(),
                    fixture.topic().id(),
                    durationMinutes
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.durationMinutes").value(durationMinutes));
    }

    @Test
    void postPersistsMultipleTopics() throws Exception {
        SessionFixture fixture = createFixture("api-multiple-topics@example.com");
        String request = """
            {
              "studentProgramId": "%s",
              "startedAt": "2026-09-07T15:00:00Z",
              "durationMinutes": 60,
              "attendanceStatus": "ATTENDED",
              "topics": [
                {"topicId": "%s", "isPrimary": true},
                {"topicId": "%s", "isPrimary": false}
              ]
            }
            """.formatted(
            fixture.studentProgram().id(),
            fixture.topic().id(),
            fixture.secondTopic().id()
        );

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.topics.length()").value(2))
            .andExpect(jsonPath("$.topics[*].topicId").value(org.hamcrest.Matchers.containsInAnyOrder(
                fixture.topic().id().toString(),
                fixture.secondTopic().id().toString()
            )));
    }

    @Test
    void getListsLessonSessionsWithPaginationAndSort() throws Exception {
        SessionFixture fixture = createFixture("api-list-session@example.com");
        createSession(fixture, 45, AttendanceStatus.ATTENDED);
        createSession(fixture, 90, AttendanceStatus.MISSED);

        mockMvc.perform(get(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .param("page", "0")
                .param("size", "1")
                .param("sort", "durationMinutes,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].durationMinutes").value(90))
            .andExpect(jsonPath("$.items[0].privateNotes").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getReturnsLessonSessionDetails() throws Exception {
        SessionFixture fixture = createFixture("api-detail-session@example.com");
        LessonSessionResult session = createSession(fixture, 60, AttendanceStatus.ATTENDED);

        mockMvc.perform(get(sessionUrl(fixture.student().getId(), session.id()))
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(session.id().toString()))
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgram().id().toString()))
            .andExpect(jsonPath("$.startedAt").isNotEmpty())
            .andExpect(jsonPath("$.privateNotes").value("Заметка"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void patchUpdatesLessonSession() throws Exception {
        SessionFixture fixture = createFixture("api-update-session@example.com");
        LessonSessionResult session = createSession(fixture, 60, AttendanceStatus.ATTENDED);

        mockMvc.perform(patch(sessionUrl(fixture.student().getId(), session.id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(session, fixture.topic().id())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.durationMinutes").value(75))
            .andExpect(jsonPath("$.attendanceStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.summary").value("Обновлённый итог"))
            .andExpect(jsonPath("$.version").value(session.version() + 1));
    }

    @Test
    void stalePatchReturnsLessonSessionVersionConflict() throws Exception {
        SessionFixture fixture = createFixture("api-stale-session@example.com");
        LessonSessionResult session = createSession(fixture, 60, AttendanceStatus.ATTENDED);
        String update = updateRequest(session, fixture.topic().id());

        mockMvc.perform(patch(sessionUrl(fixture.student().getId(), session.id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
            .andExpect(status().isOk());

        mockMvc.perform(patch(sessionUrl(fixture.student().getId(), session.id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(update))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LESSON_SESSION_VERSION_CONFLICT"))
            .andExpect(jsonPath("$.message").value("Lesson session was modified by another request"));
    }

    @Test
    void unknownLessonSessionReturnsNotFoundContract() throws Exception {
        SessionFixture fixture = createFixture("api-missing-session@example.com");

        mockMvc.perform(get(sessionUrl(fixture.student().getId(), UUID.randomUUID()))
                .with(user(fixture.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LESSON_SESSION_NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(sessionsUrl(UUID.randomUUID())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        SessionFixture fixture = createFixture("api-post-csrf@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.studentProgram().id(), fixture.topic().id(), 60)))
            .andExpect(status().isForbidden());
    }

    @Test
    void patchWithoutCsrfIsForbidden() throws Exception {
        SessionFixture fixture = createFixture("api-patch-csrf@example.com");
        LessonSessionResult session = createSession(fixture, 60, AttendanceStatus.ATTENDED);

        mockMvc.perform(patch(sessionUrl(fixture.student().getId(), session.id()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(session, fixture.topic().id())))
            .andExpect(status().isForbidden());
    }

    @Test
    void studentRoleIsForbidden() throws Exception {
        AuthenticatedUser studentPrincipal = new AuthenticatedUser(
            UUID.randomUUID(),
            "student-role@example.com",
            "password-hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get(sessionsUrl(UUID.randomUUID())).with(user(studentPrincipal)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void foreignStudentIsNormalizedToNotFound() throws Exception {
        SessionFixture owner = createFixture("api-owner@example.com");
        SessionFixture other = createFixture("api-other@example.com");

        mockMvc.perform(get(sessionsUrl(owner.student().getId()))
                .with(user(other.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));

        mockMvc.perform(post(sessionsUrl(owner.student().getId()))
                .with(user(other.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    owner.studentProgram().id(), owner.topic().id(), 60
                )))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 601})
    void invalidDurationReturnsValidationError(int durationMinutes) throws Exception {
        SessionFixture fixture = createFixture("api-invalid-duration-" + durationMinutes + "@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    fixture.studentProgram().id(), fixture.topic().id(), durationMinutes
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("durationMinutes"));
    }

    @Test
    void foreignLessonSessionIsNotReadableOrUpdatable() throws Exception {
        SessionFixture owner = createFixture("api-session-owner@example.com");
        SessionFixture other = createFixture("api-session-other@example.com");
        LessonSessionResult session = createSession(owner, 60, AttendanceStatus.ATTENDED);

        mockMvc.perform(get(sessionUrl(owner.student().getId(), session.id()))
                .with(user(other.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));

        mockMvc.perform(patch(sessionUrl(owner.student().getId(), session.id()))
                .with(user(other.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest(session, owner.topic().id())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void invalidStudentProgramReturnsNotFoundContract() throws Exception {
        SessionFixture fixture = createFixture("api-program@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(UUID.randomUUID(), fixture.topic().id(), 60)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void invalidTopicReturnsBadRequestContract() throws Exception {
        SessionFixture fixture = createFixture("api-topic@example.com");

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(fixture.studentProgram().id(), UUID.randomUUID(), 60)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("LESSON_SESSION_TOPIC_INVALID"));
    }

    @Test
    void nullTopicElementReturnsValidationError() throws Exception {
        SessionFixture fixture = createFixture("api-null-topic@example.com");
        String request = """
            {
              "studentProgramId": "%s",
              "startedAt": "2026-09-07T15:00:00Z",
              "durationMinutes": 60,
              "attendanceStatus": "ATTENDED",
              "topics": [null]
            }
            """.formatted(fixture.studentProgram().id());

        mockMvc.perform(post(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void arbitrarySortFieldIsRejected() throws Exception {
        SessionFixture fixture = createFixture("api-sort@example.com");

        mockMvc.perform(get(sessionsUrl(fixture.student().getId()))
                .with(user(fixture.principal()))
                .param("sort", "teacherId,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("sort"));
    }

    @Test
    void openApiPublishesSessionOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/sessions'].post.operationId")
                .value("createLessonSession"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/sessions'].get.operationId")
                .value("listLessonSessions"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/sessions/{sessionId}'].get.operationId")
                .value("getLessonSession"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/sessions/{sessionId}'].patch.operationId")
                .value("updateLessonSession"))
            .andExpect(jsonPath("$.components.schemas.CreateLessonSessionRequest.properties.studentProgramId.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.CreateLessonSessionRequest.properties.startedAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.components.schemas.CreateLessonSessionRequest.properties.attendanceStatus.enum.length()")
                .value(3))
            .andExpect(jsonPath("$.components.schemas.LessonSessionDetailsResponse.properties.id.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.LessonSessionDetailsResponse.properties.startedAt.format")
                .value("date-time"));
    }

    private LessonSessionResult createSession(
        SessionFixture fixture,
        int durationMinutes,
        AttendanceStatus status
    ) {
        return lessonSessionService.createLessonSession(
            fixture.principal(),
            new CreateLessonSessionCommand(
                fixture.student().getId(),
                fixture.studentProgram().id(),
                Instant.now(),
                durationMinutes,
                status,
                "Итог",
                "Заметка",
                List.of(new LessonSessionTopicInput(fixture.topic().id(), true))
            )
        );
    }

    private String createRequest(UUID studentProgramId, UUID topicId, int durationMinutes) {
        return createRequest(studentProgramId, topicId, durationMinutes, AttendanceStatus.ATTENDED);
    }

    private String createRequest(
        UUID studentProgramId,
        UUID topicId,
        int durationMinutes,
        AttendanceStatus attendanceStatus
    ) {
        return """
            {
              "studentProgramId": "%s",
              "startedAt": "2026-09-07T15:00:00Z",
              "durationMinutes": %d,
              "attendanceStatus": "%s",
              "summary": "Разобрали циклы",
              "privateNotes": "Повторить вложенные циклы",
              "topics": [{"topicId": "%s", "isPrimary": true}]
            }
            """.formatted(studentProgramId, durationMinutes, attendanceStatus, topicId);
    }

    private String updateRequest(LessonSessionResult session, UUID topicId) {
        return """
            {
              "startedAt": "%s",
              "durationMinutes": 75,
              "attendanceStatus": "CANCELLED",
              "summary": "Обновлённый итог",
              "privateNotes": "Обновлённая заметка",
              "version": %d,
              "topics": [{"topicId": "%s", "isPrimary": true}]
            }
            """.formatted(session.startedAt(), session.version(), topicId);
    }

    private SessionFixture createFixture(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), user, "Teacher"
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(),
            email,
            "password-hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), "Ученик", null, StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(),
            teacher.id(),
            null,
            "Предмет " + UUID.randomUUID(),
            null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(
            new LearningProgramEntity(
                UUID.randomUUID(),
                teacher.id(),
                subject.id(),
                "Программа",
                null,
                LearningProgramStatus.DRAFT
            )
        );
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(
            new StudentProgramEntity(
                UUID.randomUUID(),
                student.getId(),
                learningProgram.getId(),
                teacher.id(),
                StudentProgramStatus.ACTIVE,
                480,
                Instant.now(),
                null
            )
        );
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0
        ));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Тема", null, 0, TopicStatus.DRAFT
        ));
        TopicEntity secondTopic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Вторая тема", null, 1, TopicStatus.DRAFT
        ));
        return new SessionFixture(teacher, principal, student, studentProgram, topic, secondTopic);
    }

    private String sessionsUrl(UUID studentId) {
        return "/api/v1/teacher/students/" + studentId + "/sessions";
    }

    private String sessionUrl(UUID studentId, UUID sessionId) {
        return sessionsUrl(studentId) + "/" + sessionId;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record SessionFixture(
        TeacherEntity teacher,
        AuthenticatedUser principal,
        StudentEntity student,
        StudentProgramEntity studentProgram,
        TopicEntity topic,
        TopicEntity secondTopic
    ) {
    }
}
