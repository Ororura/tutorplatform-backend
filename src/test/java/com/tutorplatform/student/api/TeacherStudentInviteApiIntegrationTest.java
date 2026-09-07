package com.tutorplatform.student.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteEntity;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class TeacherStudentInviteApiIntegrationTest {

    private static final String FRONTEND_BASE_URL = "https://frontend.example.test/app";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureApplication(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.student-invites.ttl", () -> "P2D");
        registry.add("app.student-invites.public-frontend-base-url", () -> FRONTEND_BASE_URL + "/");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StudentInviteRepository studentInviteRepository;

    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanData() {
        studentInviteRepository.deleteAll();
        teacherStudentLinkRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createReturnsRawTokenOnceButStoresOnlyHashAndDoesNotLogToken(CapturedOutput output) throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        StudentEntity student = createStudent(teacher.teacher());
        Instant before = Instant.now();

        MvcResult result = createInvite(teacher, student.getId(), "  student@example.com  ")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                        "/api/v1/teacher/students/" + student.getId() + "/invites/"
                )))
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.inviteUrl").value(org.hamcrest.Matchers.startsWith(
                        FRONTEND_BASE_URL + "/invite/student/"
                )))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

        JsonNode response = json(result);
        String inviteUrl = response.required("inviteUrl").textValue();
        String rawToken = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);
        UUID inviteId = UUID.fromString(response.required("id").textValue());
        StudentInviteEntity persisted = studentInviteRepository.findById(inviteId).orElseThrow();

        assertThat(Base64.getUrlDecoder().decode(rawToken)).hasSize(32);
        assertThat(persisted.getTokenHash()).hasSize(64).doesNotContain(rawToken);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from student_invites where token_hash = ? or token_hash like ?",
                Integer.class,
                rawToken,
                "%" + rawToken + "%"
        )).isZero();
        assertThat(persisted.getExpiresAt()).isBetween(
                before.plus(2, ChronoUnit.DAYS),
                Instant.now().plus(2, ChronoUnit.DAYS)
        );
        assertThat(output.getAll()).doesNotContain(rawToken);
    }

    @Test
    void creatingAnotherInviteRevokesPreviousActiveInviteAndListNeverReturnsSecrets() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        StudentEntity student = createStudent(teacher.teacher());

        JsonNode first = json(createInvite(teacher, student.getId(), "first@example.com")
                .andExpect(status().isCreated())
                .andReturn());
        String firstRawToken = tokenFrom(first);
        JsonNode second = json(createInvite(teacher, student.getId(), "second@example.com")
                .andExpect(status().isCreated())
                .andReturn());
        String secondRawToken = tokenFrom(second);

        MvcResult listResult = mockMvc.perform(get("/api/v1/teacher/students/{studentId}/invites", student.getId())
                        .with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[1].status").value("REVOKED"))
                .andExpect(jsonPath("$.items[0].inviteUrl").doesNotExist())
                .andExpect(jsonPath("$.items[0].token").doesNotExist())
                .andExpect(jsonPath("$.items[0].tokenHash").doesNotExist())
                .andReturn();

        String listBody = listResult.getResponse().getContentAsString();
        assertThat(listBody).doesNotContain(firstRawToken).doesNotContain(secondRawToken);
    }

    @Test
    void createChecksOwnershipRegistrationStateAndUserEmailConflicts() throws Exception {
        TeacherContext owner = createTeacher("owner@example.com");
        TeacherContext other = createTeacher("other@example.com");
        StudentEntity student = createStudent(owner.teacher());

        createInvite(other, student.getId(), "student@example.com")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));

        createInvite(owner, student.getId(), "OTHER@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

        UserEntity studentUser = createUser("registered@example.com", UserRole.STUDENT);
        jdbcTemplate.update("update students set user_id = ? where id = ?", studentUser.getId(), student.getId());

        createInvite(owner, student.getId(), "available@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_ALREADY_REGISTERED"));
    }

    @Test
    void listComputesAcceptedRevokedAndExpiredStatuses() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        StudentEntity student = createStudent(teacher.teacher());
        StudentInviteEntity accepted = persistInvite(teacher.teacher(), student, "accepted@example.com", Instant.now().plusSeconds(3600));
        StudentInviteEntity revoked = persistInvite(teacher.teacher(), student, "revoked@example.com", Instant.now().plusSeconds(3600));
        StudentInviteEntity expired = persistInvite(teacher.teacher(), student, "expired@example.com", Instant.now().plusSeconds(1));
        jdbcTemplate.update(
                "update student_invites set accepted_at = created_at where id = ?",
                accepted.getId()
        );
        jdbcTemplate.update(
                "update student_invites set revoked_at = created_at where id = ?",
                revoked.getId()
        );
        jdbcTemplate.update(
                "update student_invites set created_at = now() - interval '2 seconds', expires_at = now() - interval '1 second' where id = ?",
                expired.getId()
        );

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/students/{studentId}/invites", student.getId())
                        .with(user(teacher.principal())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(statusById(json(result), accepted.getId())).isEqualTo("ACCEPTED");
        assertThat(statusById(json(result), revoked.getId())).isEqualTo("REVOKED");
        assertThat(statusById(json(result), expired.getId())).isEqualTo("EXPIRED");
    }

    @Test
    void revokeIsIdempotentAndRejectsAcceptedOrInvisibleInvites() throws Exception {
        TeacherContext owner = createTeacher("owner@example.com");
        TeacherContext other = createTeacher("other@example.com");
        StudentEntity student = createStudent(owner.teacher());
        UUID inviteId = UUID.fromString(json(createInvite(owner, student.getId(), "student@example.com")
                .andExpect(status().isCreated())
                .andReturn()).required("id").textValue());

        revoke(owner, student.getId(), inviteId).andExpect(status().isNoContent());
        revoke(owner, student.getId(), inviteId).andExpect(status().isNoContent());
        assertThat(studentInviteRepository.findById(inviteId).orElseThrow().getRevokedAt()).isNotNull();

        revoke(other, student.getId(), inviteId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        revoke(owner, student.getId(), UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_NOT_FOUND"));

        StudentInviteEntity accepted = persistInvite(owner.teacher(), student, "accepted@example.com", Instant.now().plusSeconds(3600));
        jdbcTemplate.update(
                "update student_invites set accepted_at = created_at where id = ?",
                accepted.getId()
        );
        revoke(owner, student.getId(), accepted.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDENT_INVITE_ALREADY_ACCEPTED"));
    }

    @Test
    void openApiPublishesInviteOperationIdsAndStatusEnum() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/invites'].post.operationId")
                        .value("createStudentInvite"))
                .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/invites'].get.operationId")
                        .value("listStudentInvites"))
                .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/invites/{inviteId}'].delete.operationId")
                        .value("revokeStudentInvite"))
                .andExpect(jsonPath("$.components.schemas.StudentInviteCreatedResponse.properties.id.format")
                        .value("uuid"))
                .andExpect(jsonPath("$.components.schemas.StudentInviteCreatedResponse.properties.studentId.format")
                        .value("uuid"))
                .andExpect(jsonPath("$.components.schemas.StudentInviteCreatedResponse.properties.expiresAt.format")
                        .value("date-time"))
                .andExpect(jsonPath("$.components.schemas.StudentInviteSummaryResponse.properties.createdAt.format")
                        .value("date-time"))
                .andExpect(jsonPath("$.components.schemas.StudentInviteSummaryResponse.properties.status.enum.length()")
                        .value(4))
                .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/invites/{inviteId}'].delete.responses['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"));
    }

    private org.springframework.test.web.servlet.ResultActions createInvite(
            TeacherContext teacher,
            UUID studentId,
            String email
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/teacher/students/{studentId}/invites", studentId)
                .with(user(teacher.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.createObjectNode().put("email", email).toString()));
    }

    private org.springframework.test.web.servlet.ResultActions revoke(
            TeacherContext teacher,
            UUID studentId,
            UUID inviteId
    ) throws Exception {
        return mockMvc.perform(delete("/api/v1/teacher/students/{studentId}/invites/{inviteId}", studentId, inviteId)
                .with(user(teacher.principal()))
                .with(csrf()));
    }

    private TeacherContext createTeacher(String email) {
        UserEntity user = createUser(email, UserRole.TEACHER);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                email,
                "password-hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new TeacherContext(teacher, principal);
    }

    private UserEntity createUser(String email, UserRole role) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private StudentEntity createStudent(TeacherEntity teacher) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(),
                "Андрей",
                "Иванов",
                StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private StudentInviteEntity persistInvite(
            TeacherEntity teacher,
            StudentEntity student,
            String email,
            Instant expiresAt
    ) {
        return studentInviteRepository.saveAndFlush(new StudentInviteEntity(
                UUID.randomUUID(),
                student,
                teacher,
                email,
                UUID.randomUUID().toString(),
                expiresAt
        ));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String tokenFrom(JsonNode response) {
        String url = response.required("inviteUrl").textValue();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private String statusById(JsonNode response, UUID inviteId) {
        for (JsonNode item : response.required("items")) {
            if (item.required("id").textValue().equals(inviteId.toString())) {
                return item.required("status").textValue();
            }
        }
        throw new AssertionError("Invite not found in response: " + inviteId);
    }

    private record TeacherContext(TeacherEntity teacher, AuthenticatedUser principal) {
    }
}
