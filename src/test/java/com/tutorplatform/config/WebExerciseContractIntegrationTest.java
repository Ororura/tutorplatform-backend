package com.tutorplatform.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebExerciseContractIntegrationTest extends PostgresIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        configurePostgres(registry, "test_web_exercise_contract", null);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void publishesWebRuntimeReviewCommentsAndTeacherSourceDetail() throws Exception {
        String document =
                mvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var api = json.readTree(document);
        var schemas = api.path("components").path("schemas");
        assertThat(schemas.path("ProgrammingLanguage").path("enum").toString()).contains("WEB");
        assertThat(schemas.path("TeacherSubmissionResponse").path("properties").has("sourceCode"))
                .isTrue();
        assertThat(
                        schemas.path("StudentSubmissionResponse")
                                .path("properties")
                                .has("reviewComment"))
                .isTrue();
        assertThat(
                        api.path("paths")
                                .has(
                                        "/api/v1/teacher/students/{studentId}/submissions/{submissionId}"))
                .isTrue();
        Files.writeString(Path.of("build/web-exercises-openapi.json"), document);
    }
}
