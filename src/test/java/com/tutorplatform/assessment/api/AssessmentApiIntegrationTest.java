package com.tutorplatform.assessment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.assessment.application.SaveTeacherAssessmentCommand;
import com.tutorplatform.assessment.application.TeacherAssessmentService;
import com.tutorplatform.assessment.application.exception.InvalidTeacherAssessmentScoreException;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.session.domain.LessonSessionRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AssessmentApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_assessment_api", "008");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TeacherAssessmentService assessmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private StudentProgramRepository studentProgramRepository;
    @Autowired private LessonSessionRepository lessonSessionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void putCreatesAssessmentAndGetReturnsIt() throws Exception {
        Fixture fixture = createFixture();

        MvcResult result =
                mockMvc.perform(
                                put(url(fixture))
                                        .with(user(fixture.principal()))
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(fullRequest()))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", url(fixture)))
                        .andExpect(
                                jsonPath("$.lessonSessionId").value(fixture.sessionId().toString()))
                        .andExpect(jsonPath("$.understandingScore").value(4))
                        .andExpect(jsonPath("$.independenceScore").value(3))
                        .andExpect(jsonPath("$.practiceScore").value(5))
                        .andExpect(jsonPath("$.homeworkScore").value(4))
                        .andExpect(jsonPath("$.publicComment").value("Хорошо понял циклы"))
                        .andExpect(jsonPath("$.createdAt").isNotEmpty())
                        .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                        .andReturn();

        JsonNode created = json(result);
        assertThat(created.required("id").textValue()).isNotEqualTo(fixture.sessionId().toString());

        mockMvc.perform(get(url(fixture)).with(user(fixture.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.required("id").textValue()))
                .andExpect(jsonPath("$.lessonSessionId").value(fixture.sessionId().toString()));
    }

    @Test
    void putAllowsAllNullableFieldsAndEmptyAssessment() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(
                        put(url(fixture))
                                .with(user(fixture.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.understandingScore").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.independenceScore").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.practiceScore").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.homeworkScore").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.publicComment").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void putAcceptsScoreBoundaries() throws Exception {
        Fixture fixture = createFixture();
        String request =
                """
            {"understandingScore":1,"independenceScore":5,"practiceScore":1,"homeworkScore":5}
            """;

        mockMvc.perform(
                        put(url(fixture))
                                .with(user(fixture.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @MethodSource("invalidScores")
    void putRejectsEveryInvalidScore(String field, int score) throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(
                        put(url(fixture))
                                .with(user(fixture.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"" + field + "\":" + score + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value(field));

        assertThat(countAssessments(fixture.sessionId())).isZero();
    }

    @Test
    void repeatedPutUpdatesSingletonAndPreservesIdentityAndCreatedAt() throws Exception {
        Fixture fixture = createFixture();
        JsonNode created =
                json(
                        mockMvc.perform(
                                        put(url(fixture))
                                                .with(user(fixture.principal()))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(fullRequest()))
                                .andExpect(status().isCreated())
                                .andReturn());

        JsonNode updated =
                json(
                        mockMvc.perform(
                                        put(url(fixture))
                                                .with(user(fixture.principal()))
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"understandingScore\":5,\"publicComment\":null}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.understandingScore").value(5))
                                .andExpect(
                                        jsonPath("$.independenceScore")
                                                .value(org.hamcrest.Matchers.nullValue()))
                                .andExpect(
                                        jsonPath("$.publicComment")
                                                .value(org.hamcrest.Matchers.nullValue()))
                                .andReturn());

        assertThat(updated.required("id").textValue())
                .isEqualTo(created.required("id").textValue());
        assertThat(updated.required("createdAt").textValue())
                .isEqualTo(created.required("createdAt").textValue());
        assertThat(Instant.parse(updated.required("updatedAt").textValue()))
                .isAfterOrEqualTo(Instant.parse(created.required("updatedAt").textValue()));
        assertThat(countAssessments(fixture.sessionId())).isEqualTo(1);
    }

    @Test
    void getMissingAssessmentReturnsNormalizedNotFound() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get(url(fixture)).with(user(fixture.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotReadOrChangeAssessment() throws Exception {
        Fixture owner = createFixture();
        Fixture foreign = createFixture();
        assessmentService.saveTeacherAssessment(
                owner.principal(),
                owner.studentId(),
                owner.sessionId(),
                new SaveTeacherAssessmentCommand(4, null, null, null, "owner"));

        mockMvc.perform(get(url(owner)).with(user(foreign.principal())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LESSON_SESSION_NOT_FOUND"));
        mockMvc.perform(
                        put(url(owner))
                                .with(user(foreign.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"understandingScore\":1}"))
                .andExpect(status().isNotFound());

        assertThat(
                        assessmentService
                                .getTeacherAssessment(
                                        owner.principal(), owner.studentId(), owner.sessionId())
                                .understandingScore())
                .isEqualTo(4);
    }

    @Test
    void sessionForDifferentStudentInPathIsNotFoundAndSavesNothing() throws Exception {
        Fixture owner = createFixture();
        Fixture other = createFixture();
        String wrongPath = url(owner.studentId(), other.sessionId());

        mockMvc.perform(
                        put(wrongPath)
                                .with(user(owner.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fullRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LESSON_SESSION_NOT_FOUND"));

        assertThat(countAssessments(other.sessionId())).isZero();
    }

    @Test
    void endpointsEnforceAuthenticationRoleAndCsrf() throws Exception {
        Fixture fixture = createFixture();
        AuthenticatedUser studentPrincipal =
                new AuthenticatedUser(
                        UUID.randomUUID(),
                        "student@example.com",
                        "password",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        mockMvc.perform(get(url(fixture))).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put(url(fixture))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(url(fixture)).with(user(studentPrincipal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        put(url(fixture))
                                .with(user(fixture.principal()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentSaveCreatesAtMostOneAssessment() throws Exception {
        Fixture fixture = createFixture();
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first =
                    executor.submit(
                            () -> {
                                barrier.await();
                                return assessmentService.saveTeacherAssessment(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.sessionId(),
                                        new SaveTeacherAssessmentCommand(
                                                1, null, null, null, "first"));
                            });
            var second =
                    executor.submit(
                            () -> {
                                barrier.await();
                                return assessmentService.saveTeacherAssessment(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.sessionId(),
                                        new SaveTeacherAssessmentCommand(
                                                5, null, null, null, "second"));
                            });
            first.get();
            second.get();
        }

        assertThat(countAssessments(fixture.sessionId())).isEqualTo(1);
    }

    @Test
    void applicationValidationFailureRollsBackCreate() {
        Fixture fixture = createFixture();

        assertThatThrownBy(
                        () ->
                                assessmentService.saveTeacherAssessment(
                                        fixture.principal(),
                                        fixture.studentId(),
                                        fixture.sessionId(),
                                        new SaveTeacherAssessmentCommand(
                                                0, null, null, null, null)))
                .isInstanceOf(InvalidTeacherAssessmentScoreException.class);

        assertThat(countAssessments(fixture.sessionId())).isZero();
    }

    @Test
    void saveDoesNotModifySessionOrProgressOrHomework() throws Exception {
        Fixture fixture = createFixture();
        Instant sessionUpdatedAt =
                lessonSessionRepository.findById(fixture.sessionId()).orElseThrow().updatedAt();
        int progressCount = countRows("student_topic_progress");
        int homeworkCount = countRows("homeworks");

        mockMvc.perform(
                        put(url(fixture))
                                .with(user(fixture.principal()))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(fullRequest()))
                .andExpect(status().isCreated());

        assertThat(lessonSessionRepository.findById(fixture.sessionId()).orElseThrow().updatedAt())
                .isEqualTo(sessionUpdatedAt);
        assertThat(countRows("student_topic_progress")).isEqualTo(progressCount);
        assertThat(countRows("homeworks")).isEqualTo(homeworkCount);
    }

    @Test
    void openApiContainsAssessmentOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/sessions/{sessionId}/assessment'].get.operationId")
                                .value("getTeacherAssessment"))
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/teacher/students/{studentId}/sessions/{sessionId}/assessment'].put.operationId")
                                .value("saveTeacherAssessment"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "TeacherAssessmentResponse")));
    }

    private static Stream<Arguments> invalidScores() {
        return Stream.of(
                        "understandingScore", "independenceScore", "practiceScore", "homeworkScore")
                .flatMap(field -> Stream.of(0, 6).map(score -> Arguments.of(field, score)));
    }

    private Fixture createFixture() {
        UUID suffix = UUID.randomUUID();
        String email = "assessment-api-" + suffix + "@example.com";
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
        StudentEntity student =
                studentRepository.saveAndFlush(
                        new StudentEntity(UUID.randomUUID(), "Ученик", null, StudentStatus.ACTIVE));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject =
                subjectRepository.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                null,
                                "Предмет " + suffix,
                                null,
                                SubjectStatus.ACTIVE));
        LearningProgramEntity program =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Программа",
                                null,
                                LearningProgramStatus.DRAFT));
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
        LessonSessionEntity session =
                lessonSessionRepository.saveAndFlush(
                        new LessonSessionEntity(
                                UUID.randomUUID(),
                                studentProgram.id(),
                                teacher.id(),
                                Instant.now(),
                                60,
                                AttendanceStatus.ATTENDED,
                                "Итог",
                                "Заметка"));
        return new Fixture(principal, student.getId(), session.id());
    }

    private String fullRequest() {
        return """
            {
              "understandingScore": 4,
              "independenceScore": 3,
              "practiceScore": 5,
              "homeworkScore": 4,
              "publicComment": "Хорошо понял циклы"
            }
            """;
    }

    private String url(Fixture fixture) {
        return url(fixture.studentId(), fixture.sessionId());
    }

    private String url(UUID studentId, UUID sessionId) {
        return "/api/v1/teacher/students/" + studentId + "/sessions/" + sessionId + "/assessment";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private int countAssessments(UUID sessionId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from teacher_assessments where lesson_session_id = ?",
                Integer.class,
                sessionId);
    }

    private int countRows(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private record Fixture(AuthenticatedUser principal, UUID studentId, UUID sessionId) {}
}
