package com.tutorplatform.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.api.LoginRequest;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import com.tutorplatform.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static com.tutorplatform.demo.DemoDataIds.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.demo-data.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers
class DemoDataSeederIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired private DemoDataSeedService seedService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StudentInviteTokenService tokenService;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void cleanDatabaseIsFullySeededAndSecondRunIsIdempotent() {
        DemoDataSeedService.SeedCounts before = seedService.seed().counts();
        DemoDataSeedService.SeedResult second = seedService.seed();

        assertThat(second.created()).isFalse();
        assertThat(second.counts()).isEqualTo(before);
        assertThat(before).isEqualTo(new DemoDataSeedService.SeedCounts(
            1, 3, 2, 4, 12, 6, 11, 5, 8, 6, 15, 3, 1, 1, 2
        ));
        assertThat(count("programming_task_configs")).isEqualTo(4);
        assertThat(countWhere("tasks", "task_type = 'TEXT'")).isEqualTo(4);
        assertThat(countWhere("tasks", "task_type = 'CODE'")).isEqualTo(4);
        assertThat(countWhere("students", "user_id is not null")).isEqualTo(2);
        assertThat(countWhere("student_topic_progress", "status = 'COMPLETED'")).isEqualTo(6);
        assertThat(countWhere("student_topic_progress", "status = 'IN_PROGRESS'")).isEqualTo(3);
    }

    @Test
    void credentialsTokensAndDomainConsistencyAreValid() {
        assertPassword(DemoDataAccess.TEACHER_EMAIL, DemoDataAccess.TEACHER_PASSWORD);
        assertPassword(DemoDataAccess.ALEX_EMAIL, DemoDataAccess.STUDENT_PASSWORD);
        assertPassword(DemoDataAccess.MARIA_EMAIL, DemoDataAccess.STUDENT_PASSWORD);

        List<String> hashes = jdbcTemplate.queryForList(
            "select token_hash from progress_shares union all select token_hash from report_shares",
            String.class
        );
        assertThat(hashes)
            .contains(tokenService.hash(DemoDataAccess.PROGRESS_TOKEN))
            .contains(tokenService.hash(DemoDataAccess.REPORT_TOKEN))
            .doesNotContain(DemoDataAccess.PROGRESS_TOKEN, DemoDataAccess.REPORT_TOKEN);

        assertThat(queryInt("""
            select count(*) from homeworks h
            where h.status = 'COMPLETED' and exists (
              select 1 from homework_items hi
              where hi.homework_id = h.id and hi.required
                and not exists (
                  select 1 from submissions s
                  where s.homework_item_id = hi.id and s.task_id = hi.task_id and s.status = 'PASSED'
                )
            )
            """)).isZero();
        assertThat(queryInt("""
            select count(*) from lesson_sessions s
            join student_programs sp on sp.id = s.student_program_id
            where s.teacher_id <> sp.assigned_by_teacher_id
            """)).isZero();
        assertThat(queryInt("""
            select end_cumulative_minutes from learning_periods where id = '%s'
            """.formatted(ALEX_COMPLETED_PERIOD))).isEqualTo(300);
        assertThat(queryInt("""
            select count(*) from progress_reports
            where id = '%s' and status = 'PUBLISHED' and published_at is not null
              and snapshot_schema_version = 1
            """.formatted(PUBLISHED_REPORT))).isOne();
    }

    @Test
    void realAuthenticationAndCoreApiFlowsWork() throws Exception {
        Cookie teacherSession = login(DemoDataAccess.TEACHER_EMAIL, DemoDataAccess.TEACHER_PASSWORD);
        mockMvc.perform(get("/api/v1/teacher/students").cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3));
        mockMvc.perform(get("/api/v1/teacher/students/{id}", ALEX).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Алексей"));
        mockMvc.perform(get("/api/v1/teacher/students/{id}/sessions", ALEX).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(9));
        mockMvc.perform(get("/api/v1/teacher/students/{id}/homeworks", ALEX).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(5));
        mockMvc.perform(get("/api/v1/teacher/students/{id}/progress", ALEX)
                .param("studentProgramId", ALEX_PROGRAM.toString()).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLearningMinutes").value(510));
        mockMvc.perform(get("/api/v1/teacher/reports")
                .param("studentProgramId", ALEX_PROGRAM.toString()).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"));

        Cookie studentSession = login(DemoDataAccess.ALEX_EMAIL, DemoDataAccess.STUDENT_PASSWORD);
        mockMvc.perform(get("/api/v1/student/homeworks").cookie(studentSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(5));
        mockMvc.perform(get("/api/v1/student/progress")
                .param("studentProgramId", ALEX_PROGRAM.toString()).cookie(studentSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topics.completed.length()").value(5));
    }

    @Test
    @Transactional
    void ilyaCanBeAssignedExistingTemplateThroughPublicTeacherApi() throws Exception {
        Cookie teacherSession = login(DemoDataAccess.TEACHER_EMAIL, DemoDataAccess.TEACHER_PASSWORD);
        mockMvc.perform(get("/api/v1/teacher/students/{id}/programs", ILYA).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        MvcResult templates = mockMvc.perform(get("/api/v1/teacher/programs")
                .cookie(teacherSession))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode templateList = objectMapper.readTree(templates.getResponse().getContentAsByteArray());
        JsonNode selected = null;
        for (JsonNode template : templateList) {
            if ("ACTIVE".equals(template.path("status").textValue())) {
                selected = template;
                break;
            }
        }
        assertThat(selected).isNotNull();

        CsrfExchange csrf = obtainCsrf(teacherSession);
        MvcResult assigned = mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", ILYA)
                .cookie(csrf.cookie()).header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"learningProgramId\":\"" + selected.required("id").textValue() + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();
        String studentProgramId = objectMapper.readTree(assigned.getResponse().getContentAsByteArray())
            .required("id").textValue();

        mockMvc.perform(get("/api/v1/teacher/students/{id}/programs", ILYA).cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(studentProgramId));
        mockMvc.perform(get("/api/v1/teacher/students/{studentId}/programs/{id}", ILYA, studentProgramId)
                .cookie(teacherSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void publicProgressReportAndPdfFlowsWork() throws Exception {
        mockMvc.perform(get("/api/v1/public/progress/{token}", DemoDataAccess.PROGRESS_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLearningMinutes").value(510));
        mockMvc.perform(get("/api/v1/public/reports/{token}", DemoDataAccess.REPORT_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishedAt").isNotEmpty())
            .andExpect(jsonPath("$.learningMinutes").value(300));
        mockMvc.perform(get("/api/v1/public/reports/{token}/pdf", DemoDataAccess.REPORT_TOKEN))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
    }

    private Cookie login(String email, String password) throws Exception {
        CsrfExchange csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .cookie(csrf.cookie()).header(csrf.headerName(), csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();
        Cookie authenticated = result.getResponse().getCookie("TUTOR_SESSION");
        return authenticated == null ? csrf.cookie() : authenticated;
    }

    private CsrfExchange obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return new CsrfExchange(
            json.required("headerName").textValue(), json.required("token").textValue(),
            result.getResponse().getCookie("TUTOR_SESSION")
        );
    }

    private CsrfExchange obtainCsrf(Cookie session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Cookie responseSession = result.getResponse().getCookie("TUTOR_SESSION");
        return new CsrfExchange(
            json.required("headerName").textValue(), json.required("token").textValue(),
            responseSession == null ? session : responseSession
        );
    }

    private void assertPassword(String email, String rawPassword) {
        String hash = userRepository.findByEmail(email).orElseThrow().passwordHash();
        assertThat(hash).isNotEqualTo(rawPassword).startsWith("$argon2");
        assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue();
    }

    private int count(String table) {
        return queryInt("select count(*) from " + table);
    }

    private int countWhere(String table, String condition) {
        return queryInt("select count(*) from " + table + " where " + condition);
    }

    private int queryInt(String sql) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }

    private record CsrfExchange(String headerName, String token, Cookie cookie) {}
}
