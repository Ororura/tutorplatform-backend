package com.tutorplatform.platform.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherInvitationIntegrationTest extends PostgresIntegrationTest {

    private static final String PASSWORD =
        "correct horse battery staple";

    private static final String ADMIN_EMAIL =
        "invitation-admin@example.com";

    @DynamicPropertySource
    static void configurePostgres(
        DynamicPropertyRegistry registry
    ) {
        PostgresIntegrationTest.configurePostgres(
            registry,
            "test_teacher_invitation_api",
            null
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql(
            "DELETE FROM teacher_registration_invites"
        ).update();

        jdbcClient.sql("""
            UPDATE platform_settings
            SET registration_mode = 'OPEN',
                updated_by_admin_id = NULL
            WHERE id = 1
            """).update();

        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCreatesInvitationAndPublicCanReadIt() throws Exception {
        Cookie adminSession = createAdminSession();

        CreatedInvite invite = createInvite(
            adminSession,
            "new-teacher@example.com"
        );

        mockMvc.perform(
                get("/api/v1/public/teacher-invitations/{token}", invite.token())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email")
                .value("new-teacher@example.com"))
            .andExpect(jsonPath("$.status")
                .value("ACTIVE"))
            .andExpect(jsonPath("$.expiresAt")
                .isNotEmpty());

        mockMvc.perform(
                get("/api/v1/admin/teacher-invitations")
                    .cookie(adminSession)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invitations.length()")
                .value(1))
            .andExpect(jsonPath("$.invitations[0].status")
                .value("ACTIVE"))
            .andExpect(jsonPath("$.invitations[0].invitationUrl")
                .doesNotExist());

        String storedHash = jdbcClient.sql("""
                SELECT token_hash
                FROM teacher_registration_invites
                WHERE id = :id
                """)
            .param("id", invite.id())
            .query(String.class)
            .single();

        assertThat(storedHash)
            .matches("^[0-9a-f]{64}$")
            .isNotEqualTo(invite.token());
    }

    @Test
    void invitationWorksWhenPublicRegistrationIsClosed() throws Exception {
        Cookie adminSession = createAdminSession();

        CreatedInvite invite = createInvite(
            adminSession,
            "invited-teacher@example.com"
        );

        jdbcClient.sql("""
                UPDATE platform_settings
                SET registration_mode = 'INVITE_ONLY'
                WHERE id = 1
                """)
            .update();

        CsrfExchange guest = obtainCsrf(null);

        mockMvc.perform(
                post("/api/v1/auth/register/teacher")
                    .cookie(guest.cookie())
                    .header("X-XSRF-TOKEN", guest.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "displayName", "Обычная регистрация",
                        "email", "ordinary@example.com",
                        "password", PASSWORD
                    )))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code")
                .value("REGISTRATION_INVITE_REQUIRED"));

        MvcResult acceptance = mockMvc.perform(
                post(
                    "/api/v1/public/teacher-invitations/{token}/accept",
                    invite.token()
                )
                    .cookie(guest.cookie())
                    .header("X-XSRF-TOKEN", guest.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(acceptanceJson("Новый преподаватель"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email")
                .value("invited-teacher@example.com"))
            .andExpect(jsonPath("$.displayName")
                .value("Новый преподаватель"))
            .andExpect(jsonPath("$.roles[0]")
                .value("TEACHER"))
            .andReturn();

        Cookie authenticatedSession =
            sessionCookieFrom(acceptance, guest.cookie());

        mockMvc.perform(
                get("/api/v1/auth/me")
                    .cookie(authenticatedSession)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email")
                .value("invited-teacher@example.com"));

        var user = userRepository.findByEmail(
            "invited-teacher@example.com"
        ).orElseThrow();

        assertThat(user.roles())
            .contains(UserRole.TEACHER);

        assertThat(
            passwordEncoder.matches(
                PASSWORD,
                user.passwordHash()
            )
        ).isTrue();

        assertThat(
            teacherRepository.findByUserId(user.id())
        ).isPresent();

        Boolean accepted = jdbcClient.sql("""
                SELECT accepted_at IS NOT NULL
                FROM teacher_registration_invites
                WHERE id = :id
                """)
            .param("id", invite.id())
            .query(Boolean.class)
            .single();

        assertThat(accepted).isTrue();

        CsrfExchange secondGuest = obtainCsrf(null);

        mockMvc.perform(
                post(
                    "/api/v1/public/teacher-invitations/{token}/accept",
                    invite.token()
                )
                    .cookie(secondGuest.cookie())
                    .header("X-XSRF-TOKEN", secondGuest.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(acceptanceJson("Другой преподаватель"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code")
                .value("TEACHER_INVITATION_NOT_ACTIVE"));
    }

    @Test
    void revokedInvitationCannotBeAccepted() throws Exception {
        Cookie adminSession = createAdminSession();

        CreatedInvite invite = createInvite(
            adminSession,
            "revoked-teacher@example.com"
        );

        CsrfExchange adminCsrf = obtainCsrf(adminSession);

        mockMvc.perform(
                delete(
                    "/api/v1/admin/teacher-invitations/{id}",
                    invite.id()
                )
                    .cookie(adminCsrf.cookie())
                    .header("X-XSRF-TOKEN", adminCsrf.token())
            )
            .andExpect(status().isNoContent());

        mockMvc.perform(
                get(
                    "/api/v1/public/teacher-invitations/{token}",
                    invite.token()
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status")
                .value("REVOKED"));

        assertInvitationCannotBeAccepted(invite);

        assertThat(
            userRepository.existsByEmail(
                "revoked-teacher@example.com"
            )
        ).isFalse();
    }

    @Test
    void expiredInvitationCannotBeAccepted() throws Exception {
        Cookie adminSession = createAdminSession();

        CreatedInvite invite = createInvite(
            adminSession,
            "expired-teacher@example.com"
        );

        jdbcClient.sql("""
                UPDATE teacher_registration_invites
                SET created_at = now() - interval '10 days',
                    expires_at = now() - interval '1 day'
                WHERE id = :id
                """)
            .param("id", invite.id())
            .update();

        mockMvc.perform(
                get(
                    "/api/v1/public/teacher-invitations/{token}",
                    invite.token()
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status")
                .value("EXPIRED"));

        assertInvitationCannotBeAccepted(invite);

        assertThat(
            userRepository.existsByEmail(
                "expired-teacher@example.com"
            )
        ).isFalse();
    }

    @Test
    void invitationManagementRequiresAdmin() throws Exception {
        mockMvc.perform(
                get("/api/v1/admin/teacher-invitations")
            )
            .andExpect(status().isUnauthorized());

        CsrfExchange guest = obtainCsrf(null);

        mockMvc.perform(
                post("/api/v1/admin/teacher-invitations")
                    .cookie(guest.cookie())
                    .header("X-XSRF-TOKEN", guest.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", "teacher@example.com"
                    )))
            )
            .andExpect(status().isUnauthorized());
    }

    private void assertInvitationCannotBeAccepted(
        CreatedInvite invite
    ) throws Exception {
        CsrfExchange guest = obtainCsrf(null);

        mockMvc.perform(
                post(
                    "/api/v1/public/teacher-invitations/{token}/accept",
                    invite.token()
                )
                    .cookie(guest.cookie())
                    .header("X-XSRF-TOKEN", guest.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(acceptanceJson("Новый преподаватель"))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code")
                .value("TEACHER_INVITATION_NOT_ACTIVE"));
    }

    private Cookie createAdminSession() throws Exception {
        UserEntity admin = new UserEntity(
            UUID.randomUUID(),
            ADMIN_EMAIL,
            passwordEncoder.encode(PASSWORD),
            UserStatus.ACTIVE
        );

        admin.addRole(UserRole.TEACHER);
        admin.addRole(UserRole.ADMIN);

        userRepository.saveAndFlush(admin);

        teacherRepository.saveAndFlush(
            new TeacherEntity(
                UUID.randomUUID(),
                admin,
                "Администратор"
            )
        );

        CsrfExchange csrf = obtainCsrf(null);

        MvcResult login = mockMvc.perform(
                post("/api/v1/auth/login")
                    .cookie(csrf.cookie())
                    .header("X-XSRF-TOKEN", csrf.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", ADMIN_EMAIL,
                        "password", PASSWORD
                    )))
            )
            .andExpect(status().isOk())
            .andReturn();

        return sessionCookieFrom(login, csrf.cookie());
    }

    private CreatedInvite createInvite(
        Cookie adminSession,
        String email
    ) throws Exception {
        CsrfExchange csrf = obtainCsrf(adminSession);

        MvcResult result = mockMvc.perform(
                post("/api/v1/admin/teacher-invitations")
                    .cookie(csrf.cookie())
                    .header("X-XSRF-TOKEN", csrf.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "email", email
                    )))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitationUrl")
                .isNotEmpty())
            .andReturn();

        JsonNode json = objectMapper.readTree(
            result.getResponse().getContentAsByteArray()
        );

        String url = json.required("invitationUrl").asText();

        return new CreatedInvite(
            UUID.fromString(json.required("id").asText()),
            url.substring(url.lastIndexOf('/') + 1)
        );
    }

    private String acceptanceJson(
        String displayName
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
            "displayName", displayName,
            "password", PASSWORD
        ));
    }

    private CsrfExchange obtainCsrf(
        Cookie existingSession
    ) throws Exception {
        var request = get("/api/v1/auth/csrf");

        if (existingSession != null) {
            request.cookie(existingSession);
        }

        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = objectMapper.readTree(
            result.getResponse().getContentAsByteArray()
        );

        return new CsrfExchange(
            json.required("token").asText(),
            sessionCookieFrom(result, existingSession)
        );
    }

    private Cookie sessionCookieFrom(
        MvcResult result,
        Cookie fallback
    ) {
        Cookie cookie = result.getResponse()
            .getCookie("TUTOR_SESSION");

        if (cookie != null) {
            return cookie;
        }

        assertThat(fallback)
            .as("Existing session cookie")
            .isNotNull();

        return fallback;
    }

    private record CreatedInvite(
        UUID id,
        String token
    ) {
    }

    private record CsrfExchange(
        String token,
        Cookie cookie
    ) {
    }
}
