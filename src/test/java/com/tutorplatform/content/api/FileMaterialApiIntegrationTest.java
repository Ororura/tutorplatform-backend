package com.tutorplatform.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.domain.FileAssetRepository;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@org.junit.jupiter.api.extension.ExtendWith(
        org.springframework.boot.test.system.OutputCaptureExtension.class)
class FileMaterialApiIntegrationTest extends PostgresIntegrationTest {
    private static final Path STORAGE = temporaryStorage();
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LessonMaterialService lessonMaterialService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private LearningProgramRepository learningProgramRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private FileAssetRepository assets;
    @Autowired private JdbcTemplate jdbc;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private com.tutorplatform.file.application.FileStorage storage;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_file_material_api", "008");
        registry.add("app.file-storage.provider", () -> "LOCAL");
        registry.add("app.file-storage.directory", () -> STORAGE.toString());
        registry.add("app.material-files.max-size-bytes", () -> "1024");
    }

    private static Path temporaryStorage() {
        try {
            return Files.createTempDirectory("material-api-");
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void cleanupStorage() throws Exception {
        try (var paths = Files.walk(STORAGE)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.delete(path);
        }
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            upload(
                    ContentFixture fixture,
                    String type,
                    String name,
                    String mime,
                    byte[] bytes,
                    int position) {
        return multipart(materialsUrl(fixture.topic().id()) + "/upload")
                .file(new MockMultipartFile("file", name, mime, bytes))
                .param("materialType", type)
                .param("title", "Attachment")
                .param("position", "" + position);
    }

    @ParameterizedTest
    @EnumSource(
            value = LessonMaterialType.class,
            names = {"FILE", "IMAGE"})
    void uploadPersistsObjectMetadataAndDownloadsWithoutPaths(LessonMaterialType type)
            throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        byte[] bytes =
                type == LessonMaterialType.FILE
                        ? "%PDF-1.7\nexample".getBytes()
                        : java.util.Base64.getDecoder()
                                .decode(
                                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aE0sAAAAASUVORK5CYII=");
        String mime = type == LessonMaterialType.FILE ? "application/pdf" : "image/png";
        String name = "../../duplicate." + (type == LessonMaterialType.FILE ? "pdf" : "png");
        var result =
                mockMvc.perform(
                                upload(fixture, type.name(), name, mime, bytes, 0)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.materialType").value(type.name()))
                        .andExpect(jsonPath("$.storageKey").doesNotExist())
                        .andReturn();
        UUID materialId = UUID.fromString(json(result).get("id").asText());
        var material =
                lessonMaterialService.getLessonMaterial(
                        fixture.principal(), fixture.topic().id(), materialId);
        var asset = assets.findById(material.fileAssetId()).orElseThrow();
        assertThat(asset.uploadedByTeacherId()).isEqualTo(fixture.teacher().id());
        assertThat(asset.originalFilename()).isEqualTo(name);
        assertThat(asset.mimeType()).isEqualTo(mime);
        assertThat(asset.sizeBytes()).isEqualTo(bytes.length);
        assertThat(asset.storageProvider())
                .isEqualTo(com.tutorplatform.content.domain.StorageProvider.LOCAL);
        assertThat(asset.createdAt()).isNotNull();
        assertThat(asset.sha256())
                .isEqualTo(
                        java.util.HexFormat.of()
                                .formatHex(
                                        java.security.MessageDigest.getInstance("SHA-256")
                                                .digest(bytes)));
        assertThat(UUID.fromString(asset.storageKey()).toString()).isEqualTo(asset.storageKey());
        assertThat(Files.readAllBytes(STORAGE.resolve(asset.storageKey()))).isEqualTo(bytes);
        var duplicate =
                mockMvc.perform(
                                upload(fixture, type.name(), name, mime, bytes, 1)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andReturn();
        var otherMaterial =
                lessonMaterialService.getLessonMaterial(
                        fixture.principal(),
                        fixture.topic().id(),
                        UUID.fromString(json(duplicate).get("id").asText()));
        var otherAsset = assets.findById(otherMaterial.fileAssetId()).orElseThrow();
        assertThat(otherAsset.storageKey()).isNotEqualTo(asset.storageKey());
        assertThat(Files.exists(STORAGE.resolve(otherAsset.storageKey()))).isTrue();
        mockMvc.perform(
                        get(materialUrl(fixture.topic().id(), materialId) + "/download")
                                .with(user(fixture.principal())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(content().contentType(mime))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.allOf(
                                                org.hamcrest.Matchers.startsWith(
                                                        type == LessonMaterialType.IMAGE
                                                                ? "inline;"
                                                                : "attachment;"),
                                                org.hamcrest.Matchers.not(
                                                        org.hamcrest.Matchers.containsString(
                                                                STORAGE.toString())),
                                                org.hamcrest.Matchers.not(
                                                        org.hamcrest.Matchers.containsString(
                                                                asset.storageKey())),
                                                org.hamcrest.Matchers.not(
                                                        org.hamcrest.Matchers.containsString(
                                                                "../")))));
        var stranger = createFixture(UUID.randomUUID() + "@example.com");
        mockMvc.perform(
                        get(materialUrl(fixture.topic().id(), materialId) + "/download")
                                .with(user(stranger.principal())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(materialUrl(fixture.topic().id(), materialId) + "/download"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({
        "lesson.sh,application/octet-stream,#!/bin/sh\\necho lesson",
        "lesson.py,text/plain,print('lesson')",
        "har-parser.ts,video/mp2t,export const lesson = 1"
    })
    void uploadsEducationalSourceFilesAsFile(String name, String mime, String source)
            throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        var result =
                mockMvc.perform(
                                upload(fixture, "FILE", name, mime, source.getBytes(), 0)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.materialType").value("FILE"))
                        .andReturn();

        UUID materialId = UUID.fromString(json(result).get("id").asText());
        var material =
                lessonMaterialService.getLessonMaterial(
                        fixture.principal(), fixture.topic().id(), materialId);
        var asset = assets.findById(material.fileAssetId()).orElseThrow();
        assertThat(asset.originalFilename()).isEqualTo(name);
        assertThat(asset.mimeType()).isEqualTo(mime);

        mockMvc.perform(
                        get(materialUrl(fixture.topic().id(), materialId) + "/download")
                                .with(user(fixture.principal())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(source.getBytes()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void uploadsTsxWithMissingBrowserMimeAsOctetStream() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        var result =
                mockMvc.perform(
                                upload(
                                                fixture,
                                                "FILE",
                                                "lesson.tsx",
                                                null,
                                                "export const Lesson = () => <div />;".getBytes(),
                                                0)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andReturn();

        UUID materialId = UUID.fromString(json(result).get("id").asText());
        var material =
                lessonMaterialService.getLessonMaterial(
                        fixture.principal(), fixture.topic().id(), materialId);
        var asset = assets.findById(material.fileAssetId()).orElseThrow();
        assertThat(asset.originalFilename()).isEqualTo("lesson.tsx");
        assertThat(asset.mimeType()).isEqualTo("application/octet-stream");
    }

    @Test
    void rejectsShellScriptAsImage() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        mockMvc.perform(
                        upload(
                                        fixture,
                                        "IMAGE",
                                        "lesson.sh",
                                        "text/plain",
                                        "#!/bin/sh".getBytes(),
                                        0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesSizeMimeAndImageTypeBeforeWriting() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        long count = objectCount();
        mockMvc.perform(
                        upload(fixture, "FILE", "big.txt", "text/plain", new byte[1025], 0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        mockMvc.perform(
                        upload(
                                        fixture,
                                        "FILE",
                                        "bad.exe",
                                        "application/x-executable",
                                        new byte[] {1, 2},
                                        0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        upload(fixture, "IMAGE", "fake.png", "image/png", "not a PNG".getBytes(), 0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        upload(fixture, "IMAGE", "text.txt", "text/plain", "hello".getBytes(), 0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(objectCount()).isEqualTo(count);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from file_assets where uploaded_by_teacher_id = ?",
                                Long.class,
                                fixture.teacher().id()))
                .isZero();
    }

    @Test
    void uploadRequiresOwnerAuthenticationAndCsrf() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        var stranger = createFixture(UUID.randomUUID() + "@example.com");
        long count = objectCount();
        mockMvc.perform(
                        upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                .with(user(stranger.principal()))
                                .with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                .with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                .with(user(fixture.principal())))
                .andExpect(status().isForbidden());
        assertThat(objectCount()).isEqualTo(count);
    }

    @Test
    void databaseFailureRollsBackMetadataAndDeletesObject() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        lessonMaterialService.createLessonMaterial(
                fixture.principal(),
                new CreateLessonMaterialCommand(
                        fixture.topic().id(),
                        LessonMaterialType.TEXT,
                        "Existing",
                        "text",
                        null,
                        null,
                        0));
        long count = objectCount();
        mockMvc.perform(
                        upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isConflict());
        assertThat(objectCount()).isEqualTo(count);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from file_assets where uploaded_by_teacher_id = ?",
                                Long.class,
                                fixture.teacher().id()))
                .isZero();
        assertThat(
                        lessonMaterialService.listLessonMaterials(
                                fixture.principal(), fixture.topic().id()))
                .hasSize(1);
    }

    @Test
    void storageFailureReturnsOpaqueStorageError() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        org.mockito.Mockito.doThrow(
                        new com.tutorplatform.file.application.FileStorageException(
                                new java.io.IOException("disk details")))
                .when(storage)
                .store(org.mockito.ArgumentMatchers.any());
        try {
            mockMvc.perform(
                            upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                    .with(user(fixture.principal()))
                                    .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("FILE_STORAGE_ERROR"))
                    .andExpect(jsonPath("$.message").value("File storage operation failed"));
        } finally {
            org.mockito.Mockito.reset(storage);
        }
    }

    @Test
    void failureAtCommitAlsoCompensatesObject() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        long count = objectCount();
        jdbc.execute(
                """
            CREATE FUNCTION reject_file_material_commit() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'Test commit failure'; RETURN NEW; END $$
            """);
        jdbc.execute(
                """
            CREATE CONSTRAINT TRIGGER reject_file_material_commit
            AFTER INSERT ON lesson_materials DEFERRABLE INITIALLY DEFERRED
            FOR EACH ROW EXECUTE FUNCTION reject_file_material_commit()
            """);
        try {
            mockMvc.perform(
                            upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                    .with(user(fixture.principal()))
                                    .with(csrf()))
                    .andExpect(status().is5xxServerError());
            assertThat(objectCount()).isEqualTo(count);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from file_assets where uploaded_by_teacher_id = ?",
                                    Long.class,
                                    fixture.teacher().id()))
                    .isZero();
        } finally {
            jdbc.execute("DROP TRIGGER reject_file_material_commit ON lesson_materials");
            jdbc.execute("DROP FUNCTION reject_file_material_commit()");
        }
    }

    @Test
    void failedCompensationReportsOpaqueKeyForManualCleanup(
            org.springframework.boot.test.system.CapturedOutput output) throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        lessonMaterialService.createLessonMaterial(
                fixture.principal(),
                new CreateLessonMaterialCommand(
                        fixture.topic().id(),
                        LessonMaterialType.TEXT,
                        "Existing",
                        "text",
                        null,
                        null,
                        0));
        java.util.Set<Path> before;
        try (var paths = Files.list(STORAGE)) {
            before = paths.collect(java.util.stream.Collectors.toSet());
        }
        org.mockito.Mockito.doThrow(
                        new com.tutorplatform.file.application.FileStorageException(
                                new java.io.IOException("Test delete failure")))
                .when(storage)
                .delete(org.mockito.ArgumentMatchers.anyString());
        try {
            mockMvc.perform(
                            upload(fixture, "FILE", "a.txt", "text/plain", "hello".getBytes(), 0)
                                    .with(user(fixture.principal()))
                                    .with(csrf()))
                    .andExpect(status().isConflict());
            try (var paths = Files.list(STORAGE)) {
                var remaining = paths.filter(path -> !before.contains(path)).toList();
                assertThat(remaining).hasSize(1);
                assertThat(output.getOut())
                        .contains(
                                "FILE_STORAGE_CLEANUP_REQUIRED provider=LOCAL key="
                                        + remaining.getFirst().getFileName());
            }
        } finally {
            org.mockito.Mockito.reset(storage);
            try (var paths = Files.list(STORAGE)) {
                for (Path path : paths.filter(path -> !before.contains(path)).toList())
                    Files.delete(path);
            }
        }
    }

    @Test
    void deleteFileAfterCommitAndCompactPositions() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        var first =
                mockMvc.perform(
                                upload(
                                                fixture,
                                                "FILE",
                                                "first.txt",
                                                "text/plain",
                                                "hello".getBytes(),
                                                0)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andReturn();
        var second =
                mockMvc.perform(
                                upload(
                                                fixture,
                                                "FILE",
                                                "second.txt",
                                                "text/plain",
                                                "world".getBytes(),
                                                1)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andReturn();
        UUID firstId = UUID.fromString(json(first).get("id").asText());
        UUID secondId = UUID.fromString(json(second).get("id").asText());
        UUID firstAssetId =
                lessonMaterialService
                        .getLessonMaterial(fixture.principal(), fixture.topic().id(), firstId)
                        .fileAssetId();
        String firstKey = assets.findById(firstAssetId).orElseThrow().storageKey();
        assertThat(Files.exists(STORAGE.resolve(firstKey))).isTrue();

        mockMvc.perform(
                        delete(materialUrl(fixture.topic().id(), firstId))
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(Files.exists(STORAGE.resolve(firstKey))).isFalse();
        assertThat(assets.findById(firstAssetId)).isEmpty();
        assertThat(
                        lessonMaterialService
                                .getLessonMaterial(
                                        fixture.principal(), fixture.topic().id(), secondId)
                                .position())
                .isZero();
        mockMvc.perform(
                        get(materialUrl(fixture.topic().id(), firstId))
                                .with(user(fixture.principal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRequiresOwnedTopicAndCsrf() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        var stranger = createFixture(UUID.randomUUID() + "@example.com");
        var created =
                mockMvc.perform(
                                upload(
                                                fixture,
                                                "FILE",
                                                "private.txt",
                                                "text/plain",
                                                "hello".getBytes(),
                                                0)
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andReturn();
        UUID id = UUID.fromString(json(created).get("id").asText());
        mockMvc.perform(
                        delete(materialUrl(fixture.topic().id(), id))
                                .with(user(stranger.principal()))
                                .with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        delete(materialUrl(fixture.topic().id(), id))
                                .with(user(fixture.principal())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(materialUrl(fixture.topic().id(), id)).with(user(fixture.principal())))
                .andExpect(status().isOk());
    }

    private long objectCount() throws Exception {
        try (var paths = Files.list(STORAGE)) {
            return paths.count();
        }
    }

    private ContentFixture createFixture(String email) {
        UserEntity user =
                new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher =
                teacherRepository.saveAndFlush(
                        new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        SubjectEntity subject =
                subjectRepository.saveAndFlush(
                        new SubjectEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                null,
                                "Предмет " + UUID.randomUUID(),
                                null,
                                SubjectStatus.ACTIVE));
        LearningProgramEntity learningProgram =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacher.id(),
                                subject.id(),
                                "Программа",
                                null,
                                LearningProgramStatus.DRAFT));
        ModuleEntity module =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0));
        TopicEntity topic =
                topicRepository.saveAndFlush(
                        new TopicEntity(
                                UUID.randomUUID(),
                                module.id(),
                                "Тема",
                                null,
                                0,
                                TopicStatus.DRAFT));
        AuthenticatedUser principal =
                new AuthenticatedUser(
                        user.id(),
                        email,
                        "password-hash",
                        true,
                        List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
        return new ContentFixture(principal, teacher, topic);
    }

    private String materialsUrl(UUID topicId) {
        return "/api/v1/teacher/topics/" + topicId + "/materials";
    }

    private String materialUrl(UUID topicId, UUID materialId) {
        return materialsUrl(topicId) + "/" + materialId;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record ContentFixture(
            AuthenticatedUser principal, TeacherEntity teacher, TopicEntity topic) {}
}
