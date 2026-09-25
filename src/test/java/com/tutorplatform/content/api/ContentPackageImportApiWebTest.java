package com.tutorplatform.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.config.SecurityConfig;
import com.tutorplatform.content.application.importpackage.*;
import com.tutorplatform.program.api.ProgramExceptionHandler;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.LearningProgramNotFoundException;
import com.tutorplatform.shared.api.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TeacherContentPackageImportController.class)
@Import({
    SecurityConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    ApiErrorWriter.class,
    ContentPackageImportExceptionHandler.class,
    ProgramExceptionHandler.class,
    ApiExceptionHandler.class
})
class ContentPackageImportApiWebTest {
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID CONFIRMATION_ID = UUID.randomUUID();
    private static final UUID MODULE_ID = UUID.randomUUID();
    private static final String DIGEST = "a".repeat(64);
    private static final String URL = "/api/v1/teacher/programs/" + PROGRAM_ID + "/imports";
    private static final byte[] YAML = "schemaVersion: 1".getBytes();
    private static final AuthenticatedUser TEACHER =
            new AuthenticatedUser(
                    UUID.randomUUID(),
                    "teacher",
                    "",
                    true,
                    List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

    @Autowired MockMvc mvc;
    @MockitoBean ContentPackageImportService service;
    @MockitoBean UserDetailsService userDetailsService;

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "package.yml", "text/plain", YAML);
    }

    @Test
    void createsAndReplaysStoredResponse() throws Exception {
        when(service.importPackage(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        eq(CONFIRMATION_ID),
                        eq(DIGEST),
                        argThat(bytes -> Arrays.equals(bytes, YAML))))
                .thenReturn(
                        new ContentPackageImportResult(
                                PROGRAM_ID,
                                CONFIRMATION_ID,
                                DIGEST,
                                1,
                                2,
                                3,
                                List.of(MODULE_ID),
                                false))
                .thenReturn(
                        new ContentPackageImportResult(
                                PROGRAM_ID,
                                CONFIRMATION_ID,
                                DIGEST,
                                1,
                                2,
                                3,
                                List.of(MODULE_ID),
                                true));
        var request =
                multipart(URL)
                        .file(file())
                        .param("confirmationId", CONFIRMATION_ID.toString())
                        .param("digest", DIGEST)
                        .with(user(TEACHER))
                        .with(csrf());
        var created =
                mvc.perform(request)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.programId").value(PROGRAM_ID.toString()))
                        .andExpect(jsonPath("$.confirmationId").value(CONFIRMATION_ID.toString()))
                        .andExpect(jsonPath("$.digest").value(DIGEST))
                        .andExpect(jsonPath("$.moduleCount").value(1))
                        .andExpect(jsonPath("$.topicCount").value(2))
                        .andExpect(jsonPath("$.materialCount").value(3))
                        .andExpect(jsonPath("$.createdModuleIds[0]").value(MODULE_ID.toString()))
                        .andExpect(jsonPath("$.replayed").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var replay =
                mvc.perform(
                                multipart(URL)
                                        .file(file())
                                        .param("confirmationId", CONFIRMATION_ID.toString())
                                        .param("digest", DIGEST)
                                        .with(user(TEACHER))
                                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.createdModuleIds[0]").value(MODULE_ID.toString()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(replay).isEqualTo(created);
        verify(service, times(2))
                .importPackage(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        eq(CONFIRMATION_ID),
                        eq(DIGEST),
                        argThat(bytes -> Arrays.equals(bytes, YAML)));
    }

    @Test
    void missingAndMalformedFieldsAreBadRequests() throws Exception {
        mvc.perform(
                        multipart(URL)
                                .param("confirmationId", CONFIRMATION_ID.toString())
                                .param("digest", DIGEST)
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(URL)
                                .file(file())
                                .param("digest", DIGEST)
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(URL)
                                .file(file())
                                .param("confirmationId", "bad")
                                .param("digest", DIGEST)
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(URL)
                                .file(file())
                                .param("confirmationId", CONFIRMATION_ID.toString())
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        multipart(URL)
                                .file(file())
                                .param("confirmationId", CONFIRMATION_ID.toString())
                                .param("digest", "oops")
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void securityAndProgramRulesApply() throws Exception {
        mvc.perform(multipart(URL).file(file()).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(URL).file(file()).with(user("student").roles("STUDENT")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)))
                .andExpect(status().isForbidden());
        when(service.importPackage(any(), any(), any(), any(), any()))
                .thenThrow(new LearningProgramNotFoundException())
                .thenThrow(new InvalidLearningProgramStatusException("Archived"));
        for (int status : new int[] {404, 409}) {
            mvc.perform(
                            multipart(URL)
                                    .file(file())
                                    .param("confirmationId", CONFIRMATION_ID.toString())
                                    .param("digest", DIGEST)
                                    .with(user(TEACHER))
                                    .with(csrf()))
                    .andExpect(status().is(status));
        }
    }

    @Test
    void invalidFileAndConflictsUseControlledStatuses() throws Exception {
        mvc.perform(
                        multipart(URL)
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "large.yml",
                                                "text/plain",
                                                new byte[1_048_577]))
                                .param("confirmationId", CONFIRMATION_ID.toString())
                                .param("digest", DIGEST)
                                .with(user(TEACHER))
                                .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
        when(service.importPackage(any(), any(), any(), any(), any()))
                .thenThrow(
                        new ContentPackageParseException(
                                ContentPackageParseException.Code.INVALID_YAML,
                                "root",
                                "Invalid YAML"))
                .thenThrow(new ContentPackageDigestMismatchException())
                .thenThrow(new ContentPackageConfirmationConflictException());
        for (int status : new int[] {400, 409, 409}) {
            mvc.perform(
                            multipart(URL)
                                    .file(file())
                                    .param("confirmationId", CONFIRMATION_ID.toString())
                                    .param("digest", DIGEST)
                                    .with(user(TEACHER))
                                    .with(csrf()))
                    .andExpect(status().is(status));
        }
    }
}
