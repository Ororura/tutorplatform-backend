package com.tutorplatform.content.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
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
import java.nio.charset.StandardCharsets;
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

@WebMvcTest(controllers = TeacherContentPackagePreviewController.class)
@Import({
    SecurityConfig.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    ApiErrorWriter.class,
    ContentPackagePreviewExceptionHandler.class,
    ProgramExceptionHandler.class,
    ApiExceptionHandler.class
})
class ContentPackagePreviewApiWebTest {
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final String URL = "/api/v1/teacher/programs/" + PROGRAM_ID + "/imports/preview";
    private static final AuthenticatedUser TEACHER =
            new AuthenticatedUser(
                    UUID.randomUUID(),
                    "teacher",
                    "",
                    true,
                    List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));

    @Autowired MockMvc mvc;
    @MockitoBean ContentPackagePreviewService service;
    @MockitoBean UserDetailsService userDetailsService;

    private static MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "package.yml",
                "text/plain",
                "schemaVersion: 1".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void successfulPreviewHasTypedHierarchyAndEmptyErrors() throws Exception {
        var result =
                new ContentPackagePreviewResult(
                        PROGRAM_ID,
                        "abc",
                        1,
                        1,
                        1,
                        List.of(
                                new ContentPackagePreviewResult.Module(
                                        "Module",
                                        null,
                                        List.of(
                                                new ContentPackagePreviewResult.Topic(
                                                        "Topic",
                                                        null,
                                                        List.of(
                                                                new ContentPackagePreviewResult
                                                                        .Material(
                                                                        "Notes",
                                                                        com.tutorplatform.content
                                                                                .domain
                                                                                .LessonMaterialType
                                                                                .MARKDOWN,
                                                                        "Hello",
                                                                        null)))))));
        when(service.preview(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn(result);

        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.programId").value(PROGRAM_ID.toString()))
                .andExpect(jsonPath("$.digest").value("abc"))
                .andExpect(jsonPath("$.moduleCount").value(1))
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.materialCount").value(1))
                .andExpect(
                        jsonPath("$.modules[0].topics[0].materials[0].materialType")
                                .value("MARKDOWN"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void parserAndValidatorFailuresUseOneBadRequestShape() throws Exception {
        when(service.preview(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(
                        new ContentPackageParseException(
                                ContentPackageParseException.Code.INVALID_YAML,
                                "root",
                                "Invalid YAML"))
                .thenThrow(
                        new ContentPackageValidationException(
                                new ContentPackageValidationResult(
                                        List.of(
                                                new ContentPackageValidationError(
                                                        ContentPackageValidationError.Code
                                                                .REQUIRED_FIELD,
                                                        "modules[0].title",
                                                        "Title is required")))));

        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_YAML"))
                .andExpect(jsonPath("$.errors[0].path").value("root"));
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("REQUIRED_FIELD"))
                .andExpect(jsonPath("$.errors[0].path").value("modules[0].title"))
                .andExpect(jsonPath("$.errors[0].message").value("Title is required"));
    }

    @Test
    void authenticationRoleAndCsrfAreEnforced() throws Exception {
        mvc.perform(multipart(URL).file(file()).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(URL).file(file()).with(user("student").roles("STUDENT")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void programOwnershipAndEditabilityKeepExistingStatuses() throws Exception {
        when(service.preview(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new LearningProgramNotFoundException())
                .thenThrow(
                        new InvalidLearningProgramStatusException(
                                "Archived learning program cannot be edited"));
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void missingPartIsBadRequestAndOversizeIsControlled() throws Exception {
        mvc.perform(multipart(URL).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isBadRequest());
        when(service.preview(
                        eq(TEACHER),
                        eq(PROGRAM_ID),
                        any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(
                        new ContentPackageParseException(
                                ContentPackageParseException.Code.FILE_TOO_LARGE,
                                "file",
                                "YAML file exceeds 1 MiB"));
        mvc.perform(multipart(URL).file(file()).with(user(TEACHER)).with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }
}
