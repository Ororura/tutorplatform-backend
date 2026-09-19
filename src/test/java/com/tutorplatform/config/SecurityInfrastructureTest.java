package com.tutorplatform.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.api.CsrfController;
import com.tutorplatform.shared.api.ApiErrorWriter;
import com.tutorplatform.shared.api.RestAccessDeniedHandler;
import com.tutorplatform.shared.api.RestAuthenticationEntryPoint;
import com.tutorplatform.shared.web.TraceIdFilter;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CsrfController.class)
@Import({
    SecurityConfig.class,
    SecurityInfrastructureTest.SecurityProbeController.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    ApiErrorWriter.class,
    TraceIdFilter.class
})
class SecurityInfrastructureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void unauthenticatedProtectedEndpointReturnsNormalizedUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/security-probe"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists(TraceIdFilter.HEADER))
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unsafeRequestWithoutCsrfReturnsNormalizedForbiddenError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void authenticatedPrincipalWithoutRequiredRoleReturnsNormalizedAccessDeniedError() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/security-probe")
                .with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unsafeRequestWithCsrfPassesSecurityLayer() throws Exception {
        CsrfExchange csrf = obtainCsrf();

        mockMvc.perform(post("/api/v1/auth/login")
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isNoContent());
    }

    @ParameterizedTest(name = "public auth flow requires CSRF: {0}")
    @MethodSource("unsafePublicEndpoints")
    void publicUnsafeEndpointMatrixRequiresCsrf(String path) throws Exception {
        mockMvc.perform(post(path))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        CsrfExchange csrf = obtainCsrf();
        mockMvc.perform(post(path)
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isNoContent());
    }

    @Test
    void logoutInvalidatesCurrentSession() throws Exception {
        CsrfExchange csrf = obtainCsrf();
        csrf.session().setAttribute("application-state", "present");

        mockMvc.perform(post("/api/v1/auth/logout")
                .session(csrf.session())
                .with(user("teacher").roles("TEACHER"))
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isNoContent());

        assertThatIllegalStateException()
            .isThrownBy(() -> csrf.session().getAttribute("application-state"));
    }

    @Test
    void configuredArgon2PasswordEncoderCanHashAndVerifyPasswords() {
        String encoded = passwordEncoder.encode("correct horse battery staple");

        assertThat(passwordEncoder.matches("correct horse battery staple", encoded)).isTrue();
    }

    @Test
    void productionCookieHasRequiredSecurityAttributes() {
        var response = new MockHttpServletResponse();
        var cookieSerializer = new SecurityConfig().cookieSerializer(true);

        cookieSerializer.writeCookieValue(new CookieValue(
            new MockHttpServletRequest(),
            response,
            "session-id"
        ));

        assertThat(response.getHeader("Set-Cookie"))
            .startsWith("TUTOR_SESSION=")
            .contains("Path=/")
            .contains("Secure")
            .contains("HttpOnly")
            .contains("SameSite=Lax");
    }

    @Test
    void developmentCookieCanDisableSecureThroughConfiguration() {
        var response = new MockHttpServletResponse();
        var cookieSerializer = new SecurityConfig().cookieSerializer(false);

        cookieSerializer.writeCookieValue(new CookieValue(
            new MockHttpServletRequest(), response, "session-id"
        ));

        assertThat(response.getHeader("Set-Cookie"))
            .contains("HttpOnly")
            .doesNotContain("Secure");
    }

    @ParameterizedTest(name = "{0} {1} requires CSRF")
    @MethodSource("unsafeAuthenticatedEndpoints")
    void authenticatedUnsafeEndpointMatrixRequiresCsrf(
        org.springframework.http.HttpMethod method,
        String path,
        String role
    ) throws Exception {
        var authenticated = user(role.toLowerCase()).roles(role);

        mockMvc.perform(request(method, path).with(authenticated))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        CsrfExchange csrf = obtainCsrf();
        mockMvc.perform(request(method, path)
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .with(authenticated))
            .andExpect(status().isNoContent());
    }

    @ParameterizedTest(name = "public matcher is method-specific: {0} {1}")
    @MethodSource("nonPublicMethodsUnderPublicNamespaces")
    void publicMatchersDoNotPermitUnintendedMethods(
        org.springframework.http.HttpMethod method,
        String path
    ) throws Exception {
        CsrfExchange csrf = obtainCsrf();
        mockMvc.perform(request(method, path)
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private static Stream<Arguments> unsafeAuthenticatedEndpoints() {
        String id = "00000000-0000-0000-0000-000000000001";
        return Stream.of(
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/students", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.PATCH, "/api/v1/teacher/students/" + id, "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/topics/" + id + "/materials", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/students/" + id + "/sessions", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/tasks", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/students/" + id + "/homeworks", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.PUT, "/api/v1/teacher/students/" + id + "/sessions/" + id + "/assessment", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/reports", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/students/" + id + "/progress/shares", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.DELETE, "/api/v1/teacher/reports/" + id + "/shares/" + id, "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.PATCH, "/api/v1/teacher/students/" + id + "/submissions/" + id + "/review", "TEACHER"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/student/tasks/" + id + "/submissions", "STUDENT"),
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/student/tasks/" + id + "/run", "STUDENT")
        );
    }

    private static Stream<Arguments> nonPublicMethodsUnderPublicNamespaces() {
        return Stream.of(
            Arguments.of(org.springframework.http.HttpMethod.POST, "/api/v1/public/progress/token"),
            Arguments.of(org.springframework.http.HttpMethod.DELETE, "/api/v1/public/reports/token"),
            Arguments.of(org.springframework.http.HttpMethod.DELETE, "/api/v1/public/student-invitations/token")
        );
    }

    private static Stream<Arguments> unsafePublicEndpoints() {
        return Stream.of(
            Arguments.of("/api/v1/auth/login"),
            Arguments.of("/api/v1/auth/register/teacher"),
            Arguments.of("/api/v1/auth/logout"),
            Arguments.of("/api/v1/public/student-invitations/token/accept")
        );
    }

    private CsrfExchange obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
            .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isInstanceOf(MockHttpSession.class);

        return new CsrfExchange(
            (MockHttpSession) session,
            response.required("headerName").textValue(),
            response.required("token").textValue()
        );
    }

    private record CsrfExchange(MockHttpSession session, String headerName, String token) {
    }

    @RestController
    @RequestMapping("/api/v1")
    static class SecurityProbeController {

        @GetMapping("/teacher/security-probe")
        void protectedEndpoint() {
        }

        @PostMapping({
            "/auth/login",
            "/auth/register/teacher",
            "/auth/logout",
            "/public/student-invitations/{token}/accept"
        })
        @ResponseStatus(NO_CONTENT)
        void unsafePublicEndpoint() {
        }

        @RequestMapping(
            path = {
                "/teacher/students",
                "/teacher/students/{studentId}",
                "/teacher/topics/{topicId}/materials",
                "/teacher/students/{studentId}/sessions",
                "/teacher/tasks",
                "/teacher/students/{studentId}/homeworks",
                "/teacher/students/{studentId}/sessions/{sessionId}/assessment",
                "/teacher/reports",
                "/teacher/students/{studentId}/progress/shares",
                "/teacher/reports/{reportId}/shares/{shareId}",
                "/teacher/students/{studentId}/submissions/{submissionId}/review",
                "/student/tasks/{taskId}/submissions",
                "/student/tasks/{taskId}/code-submissions",
                "/student/tasks/{taskId}/run",
                "/public/progress/{token}",
                "/public/reports/{token}",
                "/public/student-invitations/{token}"
            },
            method = {
                RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.DELETE
            }
        )
        @ResponseStatus(NO_CONTENT)
        void unsafeProbe() {
        }
    }
}
