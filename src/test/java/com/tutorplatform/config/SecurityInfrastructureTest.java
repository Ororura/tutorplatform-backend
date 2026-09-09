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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

        @PostMapping("/auth/login")
        @ResponseStatus(NO_CONTENT)
        void unsafePublicEndpoint() {
        }
    }
}
