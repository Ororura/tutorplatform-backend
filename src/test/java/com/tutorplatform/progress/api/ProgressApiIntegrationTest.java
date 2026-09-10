package com.tutorplatform.progress.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ProgressApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.target", () -> "007");
    }

    @Test
    void teacherAndStudentEndpointsReturnTheSameCompleteProgressWithoutCsrf() throws Exception {
        Fixture fixture = fixture("complete");
        UUID moduleId = module(fixture, 0);
        UUID completedTopic = topic(fixture, moduleId, 0, "Completed topic", "COMPLETED");
        UUID inProgressTopic = topic(fixture, moduleId, 1, "In progress topic", "IN_PROGRESS");
        UUID firstSession = session(fixture, "ATTENDED", 60);
        UUID secondSession = session(fixture, "ATTENDED", 60);
        session(fixture, "MISSED", 30);
        assessment(firstSession, 5, 4, null, 3);
        assessment(secondSession, 4, null, 5, 4);
        UUID completedHomework = homework(fixture, "COMPLETED");
        UUID assignedHomework = homework(fixture, "ASSIGNED");
        UUID passedTask = task(fixture, "Passed");
        UUID failedTask = task(fixture, "Failed");
        UUID passedItem = item(completedHomework, passedTask, 0);
        UUID failedItem = item(assignedHomework, failedTask, 0);
        submission(fixture, passedTask, passedItem, 1, "PASSED");
        submission(fixture, failedTask, failedItem, 1, "FAILED");

        MvcResult teacherResult = mockMvc.perform(get(teacherUrl(fixture))
                .with(user(fixture.teacherPrincipal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentProgramId").value(fixture.studentProgramId().toString()))
            .andExpect(jsonPath("$.totalLearningMinutes").value(120))
            .andExpect(jsonPath("$.sessionsCount").value(3))
            .andExpect(jsonPath("$.attendanceRate").value(2.0 / 3.0))
            .andExpect(jsonPath("$.topics.completed[0].id").value(completedTopic.toString()))
            .andExpect(jsonPath("$.topics.completed[0].title").value("Completed topic"))
            .andExpect(jsonPath("$.topics.completed[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.topics.inProgress[0].id").value(inProgressTopic.toString()))
            .andExpect(jsonPath("$.homework.assigned").value(2))
            .andExpect(jsonPath("$.homework.completed").value(1))
            .andExpect(jsonPath("$.practice.assigned").value(2))
            .andExpect(jsonPath("$.practice.completed").value(1))
            .andExpect(jsonPath("$.assessment.understandingAverage").value(4.5))
            .andExpect(jsonPath("$.assessment.independenceAverage").value(4.0))
            .andExpect(jsonPath("$.assessment.practiceAverage").value(5.0))
            .andExpect(jsonPath("$.assessment.homeworkAverage").value(3.5))
            .andReturn();

        MvcResult studentResult = mockMvc.perform(get("/api/v1/student/progress")
                .with(user(fixture.studentPrincipal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(json(studentResult)).isEqualTo(json(teacherResult));
    }

    @Test
    void teacherEndpointEnforcesAuthenticationAndTeacherRole() throws Exception {
        Fixture fixture = fixture("teacher-security");

        mockMvc.perform(get(teacherUrl(fixture))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        mockMvc.perform(get(teacherUrl(fixture))
                .with(user(fixture.studentPrincipal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void studentEndpointEnforcesAuthenticationAndStudentRole() throws Exception {
        Fixture fixture = fixture("student-security");

        mockMvc.perform(get("/api/v1/student/progress")
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        mockMvc.perform(get("/api/v1/student/progress")
                .with(user(fixture.teacherPrincipal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void ownershipFailuresAreNormalizedWithoutDisclosingForeignResources() throws Exception {
        Fixture owner = fixture("owner");
        Fixture foreign = fixture("foreign");

        mockMvc.perform(get(teacherUrl(foreign))
                .with(user(owner.teacherPrincipal()))
                .param("studentProgramId", foreign.studentProgramId().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));

        mockMvc.perform(get(teacherUrl(owner))
                .with(user(owner.teacherPrincipal()))
                .param("studentProgramId", foreign.studentProgramId().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/student/progress")
                .with(user(owner.studentPrincipal()))
                .param("studentProgramId", foreign.studentProgramId().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_NOT_FOUND"));
    }

    @Test
    void explicitProgramIdSelectsBetweenMultipleProgramsAndCannotBeOverriddenByClientIds() throws Exception {
        Fixture fixture = fixture("multiple");
        UUID secondProgramId = secondStudentProgram(fixture);
        session(fixture, secondProgramId, "ATTENDED", 45);

        mockMvc.perform(get("/api/v1/student/progress")
                .with(user(fixture.studentPrincipal()))
                .param("studentProgramId", secondProgramId.toString())
                .param("studentId", UUID.randomUUID().toString())
                .param("teacherId", UUID.randomUUID().toString())
                .param("userId", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentProgramId").value(secondProgramId.toString()))
            .andExpect(jsonPath("$.totalLearningMinutes").value(45));

        mockMvc.perform(get("/api/v1/student/progress").with(user(fixture.studentPrincipal())))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get(teacherUrl(fixture)).with(user(fixture.teacherPrincipal())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void emptyStudentProgramReturnsZeroCollectionsAndNullAssessmentAverages() throws Exception {
        Fixture fixture = fixture("empty");

        mockMvc.perform(get("/api/v1/student/progress")
                .with(user(fixture.studentPrincipal()))
                .param("studentProgramId", fixture.studentProgramId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLearningMinutes").value(0))
            .andExpect(jsonPath("$.sessionsCount").value(0))
            .andExpect(jsonPath("$.attendanceRate").value(0.0))
            .andExpect(jsonPath("$.topics.completed.length()").value(0))
            .andExpect(jsonPath("$.topics.inProgress.length()").value(0))
            .andExpect(jsonPath("$.homework.assigned").value(0))
            .andExpect(jsonPath("$.homework.completed").value(0))
            .andExpect(jsonPath("$.practice.assigned").value(0))
            .andExpect(jsonPath("$.practice.completed").value(0))
            .andExpect(jsonPath("$.assessment.understandingAverage").value((Object) null))
            .andExpect(jsonPath("$.assessment.independenceAverage").value((Object) null))
            .andExpect(jsonPath("$.assessment.practiceAverage").value((Object) null))
            .andExpect(jsonPath("$.assessment.homeworkAverage").value((Object) null));
    }

    @Test
    void openApiPublishesProgressOperationsSchemasAndRequiredProgramParameter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/progress'].get.operationId")
                .value("getTeacherStudentProgress"))
            .andExpect(jsonPath("$.paths['/api/v1/student/progress'].get.operationId")
                .value("getCurrentStudentProgress"))
            .andExpect(jsonPath("$.paths['/api/v1/student/progress'].get.parameters[0].name")
                .value("studentProgramId"))
            .andExpect(jsonPath("$.paths['/api/v1/student/progress'].get.parameters[0].required")
                .value(true))
            .andExpect(jsonPath("$.components.schemas.CurrentProgressResponse").exists())
            .andExpect(jsonPath("$.components.schemas.CurrentProgressResponse.properties.assessment").exists())
            .andExpect(jsonPath("$.components.schemas.AssessmentResponse.properties.understandingAverage.type[1]")
                .value("null"))
            .andExpect(jsonPath("$.components.schemas.AssessmentResponse.properties.practiceAverage.type[1]")
                .value("null"))
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private Fixture fixture(String suffix) {
        UUID teacherUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("INSERT INTO users(id, email) VALUES (?, ?)", teacherUserId, suffix + "-teacher-" + teacherUserId + "@example.com");
        jdbc.update("INSERT INTO users(id, email) VALUES (?, ?)", studentUserId, suffix + "-student-" + studentUserId + "@example.com");
        jdbc.update("INSERT INTO teachers(id, user_id, display_name) VALUES (?, ?, 'Teacher')", teacherId, teacherUserId);
        jdbc.update("INSERT INTO students(id, user_id, first_name) VALUES (?, ?, 'Student')", studentId, studentUserId);
        jdbc.update("INSERT INTO teacher_student_links(teacher_id, student_id) VALUES (?, ?)", teacherId, studentId);
        jdbc.update("INSERT INTO subjects(id, owner_teacher_id, name) VALUES (?, ?, ?)", subjectId, teacherId, "Subject " + subjectId);
        jdbc.update("INSERT INTO learning_programs(id, teacher_id, subject_id, title, status) VALUES (?, ?, ?, 'Program', 'ACTIVE')", learningProgramId, teacherId, subjectId);
        jdbc.update("INSERT INTO student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) VALUES (?, ?, ?, ?)", studentProgramId, studentId, learningProgramId, teacherId);
        return new Fixture(teacherUserId, studentUserId, teacherId, studentId, subjectId, learningProgramId, studentProgramId);
    }

    private UUID secondStudentProgram(Fixture fixture) {
        UUID learningProgramId = UUID.randomUUID();
        UUID studentProgramId = UUID.randomUUID();
        jdbc.update("INSERT INTO learning_programs(id, teacher_id, subject_id, title, status) VALUES (?, ?, ?, 'Second Program', 'ACTIVE')", learningProgramId, fixture.teacherId(), fixture.subjectId());
        jdbc.update("INSERT INTO student_programs(id, student_id, learning_program_id, assigned_by_teacher_id) VALUES (?, ?, ?, ?)", studentProgramId, fixture.studentId(), learningProgramId, fixture.teacherId());
        return studentProgramId;
    }

    private UUID module(Fixture fixture, int position) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO modules(id, learning_program_id, title, position) VALUES (?, ?, 'Module', ?)", id, fixture.learningProgramId(), position);
        return id;
    }

    private UUID topic(Fixture fixture, UUID moduleId, int position, String title, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id, module_id, title, position, status) VALUES (?, ?, ?, ?, 'ACTIVE')", id, moduleId, title, position);
        jdbc.update("INSERT INTO student_topic_progress(student_program_id, topic_id, status) VALUES (?, ?, ?)", fixture.studentProgramId(), id, status);
        return id;
    }

    private UUID session(Fixture fixture, String status, int minutes) {
        return session(fixture, fixture.studentProgramId(), status, minutes);
    }

    private UUID session(Fixture fixture, UUID studentProgramId, String status, int minutes) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_sessions(id, student_program_id, teacher_id, started_at, duration_minutes, attendance_status) VALUES (?, ?, ?, now(), ?, ?)", id, studentProgramId, fixture.teacherId(), minutes, status);
        return id;
    }

    private UUID homework(Fixture fixture, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO homeworks(id, student_program_id, assigned_by_teacher_id, title, status, completed_at) VALUES (?, ?, ?, 'Homework', ?, CASE WHEN ? = 'COMPLETED' THEN now() END)", id, fixture.studentProgramId(), fixture.teacherId(), status, status);
        return id;
    }

    private UUID task(Fixture fixture, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tasks(id, teacher_id, subject_id, title, description_markdown, task_type, status) VALUES (?, ?, ?, ?, 'Description', 'TEXT', 'ACTIVE')", id, fixture.teacherId(), fixture.subjectId(), title);
        return id;
    }

    private UUID item(UUID homeworkId, UUID taskId, int position) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO homework_items(id, homework_id, task_id, position) VALUES (?, ?, ?, ?)", id, homeworkId, taskId, position);
        return id;
    }

    private void submission(Fixture fixture, UUID taskId, UUID homeworkItemId, int attempt, String status) {
        jdbc.update("INSERT INTO submissions(id, student_id, student_program_id, task_id, homework_item_id, attempt_no, status) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), fixture.studentId(), fixture.studentProgramId(), taskId, homeworkItemId, attempt, status);
    }

    private void assessment(UUID sessionId, Integer understanding, Integer independence, Integer practice, Integer homework) {
        jdbc.update("INSERT INTO teacher_assessments(id, lesson_session_id, understanding_score, independence_score, practice_score, homework_score) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), sessionId, understanding, independence, practice, homework);
    }

    private String teacherUrl(Fixture fixture) {
        return "/api/v1/teacher/students/" + fixture.studentId() + "/progress";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record Fixture(
        UUID teacherUserId,
        UUID studentUserId,
        UUID teacherId,
        UUID studentId,
        UUID subjectId,
        UUID learningProgramId,
        UUID studentProgramId
    ) {
        private AuthenticatedUser teacherPrincipal() {
            return principal(teacherUserId, "ROLE_TEACHER");
        }

        private AuthenticatedUser studentPrincipal() {
            return principal(studentUserId, "ROLE_STUDENT");
        }

        private static AuthenticatedUser principal(UUID userId, String role) {
            return new AuthenticatedUser(
                userId, userId + "@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority(role))
            );
        }
    }
}
