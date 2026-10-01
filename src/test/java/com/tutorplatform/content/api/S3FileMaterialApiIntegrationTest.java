package com.tutorplatform.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.domain.FileAssetRepository;
import com.tutorplatform.content.domain.StorageProvider;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.*;
import com.tutorplatform.subject.domain.*;
import com.tutorplatform.test.PostgresIntegrationTest;
import com.tutorplatform.user.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.ContentDisposition;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/** Real metadata transactions and provider routing; only the external S3 API is mocked. */
@SpringBootTest
@AutoConfigureMockMvc
class S3FileMaterialApiIntegrationTest extends PostgresIntegrationTest {
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
    @MockitoBean private S3Client s3;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_s3_file_material_api", "008");
        registry.add("app.file-storage.provider", () -> "S3");
        registry.add("app.file-storage.s3.endpoint", () -> "http://localhost:9000");
        registry.add("app.file-storage.s3.region", () -> "us-east-1");
        registry.add("app.file-storage.s3.bucket", () -> "test-materials");
        registry.add("app.material-files.max-size-bytes", () -> "1024");
    }

    @Test
    void uploadDownloadDeletePreserveS3MetadataAndCompensation() throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        String url = "/api/v1/teacher/topics/" + fixture.topic().id() + "/materials";
        byte[] content = "lesson".getBytes(StandardCharsets.UTF_8);
        String filename = "Конспект \"урок\".txt";
        var upload =
                mockMvc.perform(
                                multipart(url + "/upload")
                                        .file(
                                                new MockMultipartFile(
                                                        "file", filename, "text/plain", content))
                                        .param("materialType", "FILE")
                                        .param("title", "Lesson")
                                        .param("position", "0")
                                        .with(user(fixture.principal()))
                                        .with(csrf()))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.storageKey").doesNotExist())
                        .andReturn();
        UUID materialId =
                UUID.fromString(
                        objectMapper
                                .readTree(upload.getResponse().getContentAsByteArray())
                                .get("id")
                                .asText());
        var material =
                lessonMaterialService.getLessonMaterial(
                        fixture.principal(), fixture.topic().id(), materialId);
        var asset = assets.findById(material.fileAssetId()).orElseThrow();
        assertThat(asset.storageProvider()).isEqualTo(StorageProvider.S3);
        assertThat(asset.storageKey()).matches("materials/[0-9a-f-]{36}");
        assertThat(asset.originalFilename()).isEqualTo(filename);
        assertThat(asset.mimeType()).isEqualTo("text/plain");
        assertThat(asset.sizeBytes()).isEqualTo(content.length);
        assertThat(asset.sha256())
                .isEqualTo(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(content)));
        assertThat(asset.uploadedByTeacherId()).isEqualTo(fixture.teacher().id());
        var put = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(put.capture(), body.capture());
        assertThat(put.getValue().bucket()).isEqualTo("test-materials");
        assertThat(put.getValue().key()).isEqualTo(asset.storageKey()).doesNotContain(filename);
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).containsExactly(content);
        }
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));
        var download =
                mockMvc.perform(
                                get(url + "/" + materialId + "/download")
                                        .with(user(fixture.principal())))
                        .andExpect(status().isOk())
                        .andExpect(content().bytes(content))
                        .andExpect(content().contentType("text/plain"))
                        .andReturn();
        assertThat(
                        ContentDisposition.parse(
                                        download.getResponse().getHeader("Content-Disposition"))
                                .getFilename())
                .isEqualTo(filename);
        var getRequest = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObjectAsBytes(getRequest.capture());
        assertThat(getRequest.getValue().key()).isEqualTo(asset.storageKey());
        assertThat(getRequest.getValue().bucket()).isEqualTo("test-materials");

        // A position collision after PUT rolls back metadata and compensates only the new key.
        mockMvc.perform(
                        multipart(url + "/upload")
                                .file(
                                        new MockMultipartFile(
                                                "file", filename, "text/plain", content))
                                .param("materialType", "FILE")
                                .param("title", "Duplicate")
                                .param("position", "0")
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isConflict());
        verify(s3, times(2)).putObject(put.capture(), any(RequestBody.class));
        String compensatedKey = put.getAllValues().getLast().key();
        assertThat(compensatedKey).isNotEqualTo(asset.storageKey());
        var deleted = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(deleted.capture());
        assertThat(deleted.getValue().key()).isEqualTo(compensatedKey);
        assertThat(
                        lessonMaterialService.listLessonMaterials(
                                fixture.principal(), fixture.topic().id()))
                .hasSize(1);

        mockMvc.perform(delete(url + "/" + materialId).with(user(fixture.principal())).with(csrf()))
                .andExpect(status().isNoContent());
        verify(s3, times(2)).deleteObject(deleted.capture());
        assertThat(deleted.getAllValues().getLast().key()).isEqualTo(asset.storageKey());
        assertThat(assets.findById(asset.id())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../notes.txt", "..\\notes.txt", "notes\r\nX-Injected: yes.txt"})
    void invalidNamesNeverReachS3(String filename) throws Exception {
        var fixture = createFixture(UUID.randomUUID() + "@example.com");
        mockMvc.perform(
                        multipart(
                                        "/api/v1/teacher/topics/"
                                                + fixture.topic().id()
                                                + "/materials/upload")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                filename,
                                                "text/plain",
                                                "hello".getBytes(StandardCharsets.UTF_8)))
                                .param("materialType", "FILE")
                                .param("title", "Lesson")
                                .param("position", "0")
                                .with(user(fixture.principal()))
                                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(s3);
        assertThat(
                        lessonMaterialService.listLessonMaterials(
                                fixture.principal(), fixture.topic().id()))
                .isEmpty();
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

    private record ContentFixture(
            AuthenticatedUser principal, TeacherEntity teacher, TopicEntity topic) {}
}
