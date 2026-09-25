package com.tutorplatform.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.test.PostgresIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContentPackageImportApiIntegrationTest extends PostgresIntegrationTest {
    private static final String PREVIEW = "/api/v1/teacher/programs/{programId}/imports/preview";
    private static final String IMPORT = "/api/v1/teacher/programs/{programId}/imports";
    private static final String YAML =
            """
            schemaVersion: 1
            kind: modules
            modules:
              - title: First module
                topics:
                  - title: First topic
                    materials:
                      - title: Notes
                        materialType: MARKDOWN
                        content: |
                          # Heading

                            indented **text**
                      - title: Code
                        materialType: CODE_EXAMPLE
                        content: |
                          if True:
                              print('yes')
                  - title: Second topic
              - title: Second module
                topics:
                  - title: Third topic
                    materials:
                      - title: Reference
                        materialType: LINK
                        externalUrl: https://example.org/reference
            """;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        configurePostgres(registry, "test_content_package_import_api", null);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private AuthenticatedUser teacher;
    private UUID teacherId;
    private UUID programId;

    @BeforeEach
    void setUp() throws Exception {
        teacher = teacher();
        programId = createProgram(teacher, teacherId);
    }

    @Test
    void previewImportPersistsOrderedContentAndExistingApisCanEditIt() throws Exception {
        JsonNode preview = preview(teacher, programId, YAML);
        assertThat(preview.path("moduleCount").asInt()).isEqualTo(2);
        assertThat(preview.path("topicCount").asInt()).isEqualTo(3);
        assertThat(preview.path("materialCount").asInt()).isEqualTo(3);
        JsonNode result =
                importPackage(
                        teacher,
                        programId,
                        UUID.randomUUID(),
                        preview.path("digest").asText(),
                        YAML,
                        201);
        assertThat(result.path("createdModuleIds")).hasSize(2);
        assertThat(counts(programId)).containsExactly(2, 3, 3, 1);
        assertThat(
                        jdbc.queryForList(
                                "SELECT position FROM modules WHERE learning_program_id = ? ORDER BY position",
                                Integer.class,
                                programId))
                .containsExactly(0, 1);
        assertThat(
                        jdbc.queryForList(
                                "SELECT t.position FROM topics t JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ? ORDER BY m.position, t.position",
                                Integer.class,
                                programId))
                .containsExactly(0, 1, 0);
        assertThat(
                        jdbc.queryForList(
                                "SELECT lm.position FROM lesson_materials lm JOIN topics t ON t.id = lm.topic_id JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ? ORDER BY m.position, t.position, lm.position",
                                Integer.class,
                                programId))
                .containsExactly(0, 1, 0);
        assertThat(
                        jdbc.queryForList(
                                "SELECT content FROM lesson_materials WHERE title = 'Notes' AND topic_id IN (SELECT t.id FROM topics t JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ?)",
                                String.class,
                                programId))
                .containsExactly("# Heading\n\n  indented **text**\n");
        assertThat(
                        jdbc.queryForList(
                                "SELECT content FROM lesson_materials WHERE title = 'Code' AND topic_id IN (SELECT t.id FROM topics t JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ?)",
                                String.class,
                                programId))
                .containsExactly("if True:\n    print('yes')\n");
        assertThat(
                        jdbc.queryForList(
                                "SELECT external_url FROM lesson_materials WHERE title = 'Reference' AND topic_id IN (SELECT t.id FROM topics t JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ?)",
                                String.class,
                                programId))
                .containsExactly("https://example.org/reference");

        JsonNode details =
                mapper.readTree(
                        mvc.perform(
                                        get("/api/v1/teacher/programs/{programId}", programId)
                                                .with(user(teacher)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        assertThat(details.path("modules")).hasSize(2);
        assertThat(details.path("modules").get(0).path("topics")).hasSize(2);
        UUID moduleId = UUID.fromString(result.path("createdModuleIds").get(0).asText());
        UUID topicId =
                UUID.fromString(
                        details.path("modules").get(0).path("topics").get(0).path("id").asText());
        mvc.perform(
                        patch(
                                        "/api/v1/teacher/programs/{programId}/modules/{moduleId}",
                                        programId,
                                        moduleId)
                                .with(user(teacher))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"title\":\"Edited module\",\"description\":null}"))
                .andExpect(status().isOk());
        mvc.perform(
                        patch(
                                        "/api/v1/teacher/programs/{programId}/modules/{moduleId}/topics/{topicId}",
                                        programId,
                                        moduleId,
                                        topicId)
                                .with(user(teacher))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"title\":\"Edited topic\",\"description\":null,\"status\":\"DRAFT\",\"version\":"
                                                + details.path("modules")
                                                        .get(0)
                                                        .path("topics")
                                                        .get(0)
                                                        .path("version")
                                                        .asLong()
                                                + "}"))
                .andExpect(status().isOk());
        UUID materialId =
                jdbc.queryForObject(
                        "SELECT id FROM lesson_materials WHERE topic_id = ? AND position = 0",
                        UUID.class,
                        topicId);
        mvc.perform(
                        patch(
                                        "/api/v1/teacher/topics/{topicId}/materials/{materialId}",
                                        topicId,
                                        materialId)
                                .with(user(teacher))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"materialType\":\"MARKDOWN\",\"title\":\"Edited notes\",\"content\":\"# Updated\",\"position\":0,\"version\":0}"))
                .andExpect(status().isOk());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM modules WHERE id = ?", String.class, moduleId))
                .isEqualTo("Edited module");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM topics WHERE id = ?", String.class, topicId))
                .isEqualTo("Edited topic");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT content FROM lesson_materials WHERE id = ?",
                                String.class,
                                materialId))
                .isEqualTo("# Updated");
    }

    @Test
    void appendPreservesOldPositions() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/v1/teacher/programs/{programId}/modules", programId)
                                    .with(user(teacher))
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("{\"title\":\"Existing " + i + "\"}"))
                    .andExpect(status().isCreated());
        }
        importPackage(
                teacher,
                programId,
                UUID.randomUUID(),
                preview(teacher, programId, YAML).path("digest").asText(),
                YAML,
                201);
        assertThat(
                        jdbc.queryForList(
                                "SELECT position FROM modules WHERE learning_program_id = ? ORDER BY position",
                                Integer.class,
                                programId))
                .containsExactly(0, 1, 2, 3);
        assertThat(
                        jdbc.queryForList(
                                "SELECT title FROM modules WHERE learning_program_id = ? ORDER BY position",
                                String.class,
                                programId))
                .containsExactly("Existing 0", "Existing 1", "First module", "Second module");
    }

    @Test
    void digestMismatchAndInvalidYamlLeaveDatabaseUntouched() throws Exception {
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        importPackage(
                teacher,
                programId,
                UUID.randomUUID(),
                digest,
                YAML.replace("First module", "Changed module"),
                409);
        importPackage(teacher, programId, UUID.randomUUID(), digest, "schemaVersion: [", 400);
        assertThat(counts(programId)).containsExactly(0, 0, 0, 0);
    }

    @Test
    void replaySeparateConfirmationAndConflictingConfirmation() throws Exception {
        UUID confirmation = UUID.randomUUID();
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        JsonNode first = importPackage(teacher, programId, confirmation, digest, YAML, 201);
        JsonNode replay = importPackage(teacher, programId, confirmation, digest, YAML, 200);
        assertThat(replay).isEqualTo(first);
        assertThat(counts(programId)).containsExactly(2, 3, 3, 1);
        String changed = YAML.replace("First module", "Changed module");
        importPackage(
                teacher,
                programId,
                confirmation,
                preview(teacher, programId, changed).path("digest").asText(),
                changed,
                409);
        assertThat(counts(programId)).containsExactly(2, 3, 3, 1);
        JsonNode second = importPackage(teacher, programId, UUID.randomUUID(), digest, YAML, 201);
        assertThat(second.path("createdModuleIds")).isNotEqualTo(first.path("createdModuleIds"));
        assertThat(counts(programId)).containsExactly(4, 6, 6, 2);
    }

    @Test
    void ownershipEditabilityAndSecurityAreEnforced() throws Exception {
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        UUID confirmation = UUID.randomUUID();
        mvc.perform(
                        multipart(IMPORT, programId)
                                .file(file(YAML))
                                .param("confirmationId", confirmation.toString())
                                .param("digest", digest)
                                .with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        multipart(IMPORT, programId)
                                .file(file(YAML))
                                .param("confirmationId", confirmation.toString())
                                .param("digest", digest)
                                .with(user("student").roles("STUDENT"))
                                .with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        multipart(IMPORT, programId)
                                .file(file(YAML))
                                .param("confirmationId", confirmation.toString())
                                .param("digest", digest)
                                .with(user(teacher)))
                .andExpect(status().isForbidden());
        UUID ownerId = teacherId;
        AuthenticatedUser other = teacher();
        importPackage(other, programId, confirmation, digest, YAML, 404);
        jdbc.update("UPDATE learning_programs SET status = 'ARCHIVED' WHERE id = ?", programId);
        importPackage(teacher, programId, confirmation, digest, YAML, 409);
        jdbc.update("UPDATE learning_programs SET status = 'DRAFT' WHERE id = ?", programId);
        UUID studentId = UUID.randomUUID();
        UUID studentUser = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, email) VALUES (?, ?)",
                studentUser,
                studentUser + "@example.com");
        jdbc.update(
                "INSERT INTO students (id, user_id, first_name) VALUES (?, ?, 'Student')",
                studentId,
                studentUser);
        jdbc.update(
                "INSERT INTO student_programs (id, student_id, learning_program_id, assigned_by_teacher_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                studentId,
                programId,
                ownerId);
        importPackage(teacher, programId, confirmation, digest, YAML, 409);
        assertThat(counts(programId)).containsExactly(0, 0, 0, 0);
    }

    @Test
    void databaseFailureOnLateMaterialRollsBackAllInsertedContentAndConfirmation()
            throws Exception {
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        jdbc.execute(
                "ALTER TABLE lesson_materials ADD CONSTRAINT test_import_late_material CHECK (title <> 'Reference') NOT VALID");
        try {
            try {
                JsonNode failure =
                        importPackage(teacher, programId, UUID.randomUUID(), digest, YAML, 409);
                assertThat(failure.path("code").asText())
                        .isEqualTo("LESSON_MATERIAL_POSITION_CONFLICT");
            } catch (Exception exception) {
                // The database constraint may be propagated directly by MockMvc.
                assertThat(exception).hasMessageContaining("test_import_late_material");
            }
        } finally {
            jdbc.execute("ALTER TABLE lesson_materials DROP CONSTRAINT test_import_late_material");
        }
        assertThat(counts(programId)).containsExactly(0, 0, 0, 0);
    }

    @Test
    void concurrentRetryReturnsTheSameCommittedImport() throws Exception {
        UUID confirmation = UUID.randomUUID();
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        List<JsonNode> results = concurrentImports(confirmation, confirmation, digest);
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(results.get(1));
        assertThat(counts(programId)).containsExactly(2, 3, 3, 1);
    }

    @Test
    void concurrentSeparateConfirmationsAppendWithoutLostPositions() throws Exception {
        String digest = preview(teacher, programId, YAML).path("digest").asText();
        List<JsonNode> results = concurrentImports(UUID.randomUUID(), UUID.randomUUID(), digest);
        assertThat(results.get(0).path("createdModuleIds"))
                .isNotEqualTo(results.get(1).path("createdModuleIds"));
        assertThat(
                        jdbc.queryForList(
                                "SELECT position FROM modules WHERE learning_program_id = ? ORDER BY position",
                                Integer.class,
                                programId))
                .containsExactly(0, 1, 2, 3);
        assertThat(counts(programId)).containsExactly(4, 6, 6, 2);
    }

    @Test
    void openApiDescribesImportMultipartContract() throws Exception {
        JsonNode document =
                mapper.readTree(
                        mvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        JsonNode operation =
                document.path("paths")
                        .path("/api/v1/teacher/programs/{programId}/imports")
                        .path("post");
        assertThat(operation.path("operationId").asText()).isEqualTo("importTutorContentPackage");
        JsonNode body =
                operation
                        .path("requestBody")
                        .path("content")
                        .path("multipart/form-data")
                        .path("schema");
        if (body.has("$ref")) {
            body =
                    document.path("components")
                            .path("schemas")
                            .path(body.path("$ref").asText().replace("#/components/schemas/", ""));
        }
        assertThat(body.path("properties").path("file").path("format").asText())
                .isEqualTo("binary");
        assertThat(body.path("properties").path("confirmationId").path("format").asText())
                .isEqualTo("uuid");
        assertThat(body.path("properties").path("digest").path("type").asText())
                .isEqualTo("string");
    }

    private List<JsonNode> concurrentImports(UUID firstId, UUID secondId, String digest)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            var first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return importPackage(teacher, programId, firstId, digest, YAML, -1);
                            });
            var second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                return importPackage(
                                        teacher, programId, secondId, digest, YAML, -1);
                            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
    }

    private JsonNode preview(AuthenticatedUser principal, UUID id, String yaml) throws Exception {
        return mapper.readTree(
                mvc.perform(
                                multipart(PREVIEW, id)
                                        .file(file(yaml))
                                        .with(user(principal))
                                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    private JsonNode importPackage(
            AuthenticatedUser principal,
            UUID id,
            UUID confirmation,
            String digest,
            String yaml,
            int expectedStatus)
            throws Exception {
        var result =
                mvc.perform(
                        multipart(IMPORT, id)
                                .file(file(yaml))
                                .param("confirmationId", confirmation.toString())
                                .param("digest", digest)
                                .with(user(principal))
                                .with(csrf()));
        if (expectedStatus >= 0) result.andExpect(status().is(expectedStatus));
        else assertThat(result.andReturn().getResponse().getStatus()).isIn(200, 201);
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static MockMultipartFile file(String yaml) {
        return new MockMultipartFile(
                "file", "package.yaml", "application/yaml", yaml.getBytes(StandardCharsets.UTF_8));
    }

    private List<Integer> counts(UUID id) {
        return List.of(
                jdbc.queryForObject(
                        "SELECT count(*) FROM modules WHERE learning_program_id = ?",
                        Integer.class,
                        id),
                jdbc.queryForObject(
                        "SELECT count(*) FROM topics t JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ?",
                        Integer.class,
                        id),
                jdbc.queryForObject(
                        "SELECT count(*) FROM lesson_materials lm JOIN topics t ON t.id = lm.topic_id JOIN modules m ON m.id = t.module_id WHERE m.learning_program_id = ?",
                        Integer.class,
                        id),
                jdbc.queryForObject(
                        "SELECT count(*) FROM content_package_imports WHERE learning_program_id = ?",
                        Integer.class,
                        id));
    }

    private AuthenticatedUser teacher() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email) VALUES (?, ?)", userId, userId + "@example.com");
        jdbc.update(
                "INSERT INTO teachers (id, user_id, display_name) VALUES (?, ?, 'Teacher')",
                id,
                userId);
        teacherId = id;
        return new AuthenticatedUser(
                userId,
                userId + "@example.com",
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private UUID createProgram(AuthenticatedUser principal, UUID ownerId) throws Exception {
        UUID subjectId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO subjects (id, owner_teacher_id, name) VALUES (?, ?, ?)",
                subjectId,
                ownerId,
                "Subject " + subjectId);
        JsonNode result =
                mapper.readTree(
                        mvc.perform(
                                        post("/api/v1/teacher/programs")
                                                .with(user(principal))
                                                .with(csrf())
                                                .contentType("application/json")
                                                .content(
                                                        "{\"subjectId\":\""
                                                                + subjectId
                                                                + "\",\"title\":\"Import program\"}"))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        return UUID.fromString(result.path("id").asText());
    }
}
