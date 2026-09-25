package com.tutorplatform.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentPackagePreviewApiIntegrationTest extends PostgresIntegrationTest {
    private static final String URL = "/api/v1/teacher/programs/{programId}/imports/preview";
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
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(
                registry, "test_content_package_preview_api", null);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired TeacherRepository teachers;
    @Autowired SubjectRepository subjects;
    @Autowired LearningProgramRepository programs;

    private AuthenticatedUser teacher;
    private UUID programId;

    @BeforeEach
    void setUp() {
        TeacherEntity owner = teacher("owner@example.com");
        teacher = principal(owner);
        programId = program(owner, LearningProgramStatus.DRAFT);
    }

    @Test
    void validYamlReturnsCountsHierarchyAndDigest() throws Exception {
        byte[] bytes = YAML.getBytes(StandardCharsets.UTF_8);
        JsonNode result = preview(bytes);
        assertThat(result.required("valid").asBoolean()).isTrue();
        assertThat(result.required("programId").asText()).isEqualTo(programId.toString());
        assertThat(result.required("moduleCount").asInt()).isEqualTo(2);
        assertThat(result.required("topicCount").asInt()).isEqualTo(3);
        assertThat(result.required("materialCount").asInt()).isEqualTo(3);
        assertThat(result.required("digest").asText())
                .isEqualTo(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertThat(result.required("errors")).isEmpty();
        assertThat(
                        result.required("modules")
                                .get(1)
                                .required("topics")
                                .get(0)
                                .required("materials")
                                .get(0)
                                .required("externalUrl")
                                .asText())
                .isEqualTo("https://example.org/reference");
    }

    @Test
    void multipleModulesPreserveModuleAndTopicOrder() throws Exception {
        JsonNode modules = preview(YAML.getBytes(StandardCharsets.UTF_8)).required("modules");
        assertThat(modules.get(0).required("title").asText()).isEqualTo("First module");
        assertThat(modules.get(1).required("title").asText()).isEqualTo("Second module");
        assertThat(modules.get(0).required("topics").get(0).required("title").asText())
                .isEqualTo("First topic");
        assertThat(modules.get(0).required("topics").get(1).required("title").asText())
                .isEqualTo("Second topic");
        assertThat(modules.get(1).required("topics").get(0).required("title").asText())
                .isEqualTo("Third topic");
    }

    @Test
    void markdownAndPythonFormattingSurviveResponseSerialization() throws Exception {
        JsonNode materials =
                preview(YAML.getBytes(StandardCharsets.UTF_8))
                        .required("modules")
                        .get(0)
                        .required("topics")
                        .get(0)
                        .required("materials");
        assertThat(materials.get(0).required("materialType").asText()).isEqualTo("MARKDOWN");
        assertThat(materials.get(0).required("content").asText())
                .isEqualTo("# Heading\n\n  indented **text**\n");
        assertThat(materials.get(1).required("materialType").asText()).isEqualTo("CODE_EXAMPLE");
        assertThat(materials.get(1).required("content").asText())
                .isEqualTo("if True:\n    print('yes')\n");
    }

    @Test
    void unknownMaterialTypeReturnsValidationPath() throws Exception {
        invalid(
                YAML.replace("materialType: MARKDOWN", "materialType: UNKNOWN"),
                "INVALID_MATERIAL_TYPE",
                "modules[0].topics[0].materials[0].materialType");
    }

    @Test
    void emptyTitleReturnsValidationPath() throws Exception {
        invalid(
                YAML.replace("title: First module", "title: ' '"),
                "REQUIRED_FIELD",
                "modules[0].title");
    }

    @Test
    void invalidUrlReturnsValidationPath() throws Exception {
        invalid(
                YAML.replace("https://example.org/reference", "javascript:alert(1)"),
                "INVALID_EXTERNAL_URL",
                "modules[1].topics[0].materials[0].externalUrl");
    }

    @Test
    void duplicateYamlKeysAreRejectedByParser() throws Exception {
        invalid(
                YAML.replace("kind: modules", "kind: modules\nkind: modules"),
                "DUPLICATE_KEY",
                "kind");
    }

    @Test
    void oversizedUploadReturnsPayloadTooLarge() throws Exception {
        byte[] bytes = new byte[1_048_577];
        mvc.perform(multipart(URL, programId).file(file(bytes)).with(user(teacher)).with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void anonymousUserGetsUnauthorized() throws Exception {
        mvc.perform(multipart(URL, programId).file(file(YAML)).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonTeacherGetsForbidden() throws Exception {
        mvc.perform(
                        multipart(URL, programId)
                                .file(file(YAML))
                                .with(user("student").roles("STUDENT"))
                                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void foreignProgramIsHidden() throws Exception {
        UUID foreignId = program(teacher("other@example.com"), LearningProgramStatus.DRAFT);
        mvc.perform(multipart(URL, foreignId).file(file(YAML)).with(user(teacher)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
    }

    @Test
    void archivedProgramReturnsConflict() throws Exception {
        UUID archivedId =
                program(
                        teachers.findByUserId(teacher.id()).orElseThrow(),
                        LearningProgramStatus.ARCHIVED);
        mvc.perform(multipart(URL, archivedId).file(file(YAML)).with(user(teacher)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_STATUS_CONFLICT"));
    }

    @Test
    void missingCsrfTokenGetsForbidden() throws Exception {
        mvc.perform(multipart(URL, programId).file(file(YAML)).with(user(teacher)))
                .andExpect(status().isForbidden());
    }

    @Test
    void previewDoesNotWriteLearningContent() throws Exception {
        List<Long> before = counts();
        preview(YAML.getBytes(StandardCharsets.UTF_8));
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    void repeatedPreviewIsStableAndDoesNotWriteLearningContent() throws Exception {
        List<Long> before = counts();
        JsonNode first = preview(YAML.getBytes(StandardCharsets.UTF_8));
        JsonNode second = preview(YAML.getBytes(StandardCharsets.UTF_8));
        assertThat(second.required("modules")).isEqualTo(first.required("modules"));
        assertThat(second.required("digest")).isEqualTo(first.required("digest"));
        assertThat(counts()).isEqualTo(before);
    }

    private JsonNode preview(byte[] yaml) throws Exception {
        byte[] response =
                mvc.perform(
                                multipart(URL, programId)
                                        .file(file(yaml))
                                        .with(user(teacher))
                                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        return mapper.readTree(response);
    }

    private void invalid(String yaml, String code, String path) throws Exception {
        mvc.perform(multipart(URL, programId).file(file(yaml)).with(user(teacher)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value(code))
                .andExpect(jsonPath("$.errors[0].path").value(path));
    }

    private static MockMultipartFile file(String yaml) {
        return file(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile file(byte[] yaml) {
        return new MockMultipartFile("file", "package.yaml", "application/yaml", yaml);
    }

    private List<Long> counts() {
        return List.of(count("modules"), count("topics"), count("lesson_materials"));
    }

    private long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    private TeacherEntity teacher(String email) {
        UserEntity account = new UserEntity(UUID.randomUUID(), email, "hash", UserStatus.ACTIVE);
        account.addRole(UserRole.TEACHER);
        users.saveAndFlush(account);
        return teachers.saveAndFlush(new TeacherEntity(UUID.randomUUID(), account, "Teacher"));
    }

    private AuthenticatedUser principal(TeacherEntity owner) {
        UserEntity account = users.findById(owner.userId()).orElseThrow();
        return new AuthenticatedUser(
                account.id(),
                account.email(),
                "hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
    }

    private UUID program(TeacherEntity owner, LearningProgramStatus status) {
        SubjectEntity subject =
                subjects.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                owner.id(),
                                null,
                                "Subject " + UUID.randomUUID(),
                                null,
                                SubjectStatus.ACTIVE));
        return programs.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                owner.id(),
                                subject.id(),
                                "Program",
                                null,
                                status))
                .getId();
    }
}
