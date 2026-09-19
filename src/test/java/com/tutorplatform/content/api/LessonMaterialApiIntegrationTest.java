package com.tutorplatform.content.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialResult;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.domain.*;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.user.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LessonMaterialApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_lesson_material_api", "008");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LessonMaterialService lessonMaterialService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private TopicRepository topicRepository;

    @Test
    void postCreatesMarkdownMaterial() throws Exception {
        ContentFixture fixture = createFixture("api-markdown-material@example.com");

        MvcResult result = mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("MARKDOWN", "Цикл for", "# Цикл", null, 0)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/materials/")))
            .andExpect(jsonPath("$.topicId").value(fixture.topic().id().toString()))
            .andExpect(jsonPath("$.materialType").value("MARKDOWN"))
            .andExpect(jsonPath("$.title").value("Цикл for"))
            .andExpect(jsonPath("$.content").value("# Цикл"))
            .andExpect(jsonPath("$.position").value(0))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andReturn();

        assertThat(json(result).required("id").textValue()).isNotBlank();
    }

    @Test
    void postCreatesTextMaterial() throws Exception {
        ContentFixture fixture = createFixture("api-text-material@example.com");

        mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("TEXT", "Конспект", "Текст урока", null, 0)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.materialType").value("TEXT"))
            .andExpect(jsonPath("$.content").value("Текст урока"));
    }

    @Test
    void postCreatesLinkMaterial() throws Exception {
        ContentFixture fixture = createFixture("api-link-material@example.com");

        mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(
                    "LINK", "Документация Python", null, "https://docs.python.org/3/", 0
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.materialType").value("LINK"))
            .andExpect(jsonPath("$.content").doesNotExist())
            .andExpect(jsonPath("$.externalUrl").value("https://docs.python.org/3/"));
    }

    @Test
    void getListsMaterialsByPosition() throws Exception {
        ContentFixture fixture = createFixture("api-list-materials@example.com");
        LessonMaterialResult second = createMaterial(fixture, "Второй", 1);
        LessonMaterialResult first = createMaterial(fixture, "Первый", 0);

        mockMvc.perform(get(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(first.id().toString()))
            .andExpect(jsonPath("$[0].position").value(0))
            .andExpect(jsonPath("$[1].id").value(second.id().toString()))
            .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void getReturnsMaterialDetail() throws Exception {
        ContentFixture fixture = createFixture("api-get-material@example.com");
        LessonMaterialResult material = createMaterial(fixture, "Материал", 0);

        mockMvc.perform(get(materialUrl(fixture.topic().id(), material.id()))
                .with(user(fixture.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(material.id().toString()))
            .andExpect(jsonPath("$.topicId").value(fixture.topic().id().toString()))
            .andExpect(jsonPath("$.title").value("Материал"));
    }

    @Test
    void patchUpdatesMaterial() throws Exception {
        ContentFixture fixture = createFixture("api-patch-material@example.com");
        LessonMaterialResult material = createMaterial(fixture, "Материал", 0);
        String request = """
            {
              "materialType": "LINK",
              "title": "Документация",
              "content": null,
              "externalUrl": "https://example.com/updated",
              "position": 2,
              "version": %d
            }
            """.formatted(material.version());

        mockMvc.perform(patch(materialUrl(fixture.topic().id(), material.id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.materialType").value("LINK"))
            .andExpect(jsonPath("$.title").value("Документация"))
            .andExpect(jsonPath("$.content").doesNotExist())
            .andExpect(jsonPath("$.externalUrl").value("https://example.com/updated"))
            .andExpect(jsonPath("$.position").value(2))
            .andExpect(jsonPath("$.version").value(material.version() + 1));
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(materialsUrl(UUID.randomUUID())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void postAndPatchWithoutCsrfAreForbidden() throws Exception {
        ContentFixture fixture = createFixture("api-material-csrf@example.com");
        LessonMaterialResult material = createMaterial(fixture, "Материал", 0);

        mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("TEXT", "Новый", "Содержимое", null, 1)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(patch(materialUrl(fixture.topic().id(), material.id()))
                .with(user(fixture.principal()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateTextRequest(material)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void studentRoleCannotAccessTeacherMaterials() throws Exception {
        AuthenticatedUser studentPrincipal = new AuthenticatedUser(
            UUID.randomUUID(),
            "student-material-role@example.com",
            "password-hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get(materialsUrl(UUID.randomUUID())).with(user(studentPrincipal)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void foreignTopicIsNormalizedToNotFound() throws Exception {
        ContentFixture owner = createFixture("api-material-owner@example.com");
        ContentFixture foreign = createFixture("api-material-foreign@example.com");

        mockMvc.perform(get(materialsUrl(owner.topic().id()))
                .with(user(foreign.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
    }

    @Test
    void invalidBodyReturnsValidationError() throws Exception {
        ContentFixture fixture = createFixture("api-invalid-material@example.com");

        mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("TEXT", "", "Содержимое", null, -1)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[*].field")
                .value(org.hamcrest.Matchers.hasItems("title", "position")));
    }

    @ParameterizedTest
    @EnumSource(value = LessonMaterialType.class, names = {"FILE", "IMAGE"})
    void fileAndImageCreationAreRejected(LessonMaterialType type) throws Exception {
        ContentFixture fixture = createFixture(
            "api-unsupported-" + type.name().toLowerCase() + "@example.com"
        );

        mockMvc.perform(post(materialsUrl(fixture.topic().id()))
                .with(user(fixture.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(type.name(), "Unsupported", null, null, 0)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("materialType"));
    }

    @Test
    void openApiPublishesMaterialOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/materials'].post.operationId")
                .value("createLessonMaterial"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/materials'].get.operationId")
                .value("listLessonMaterials"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/materials/{materialId}'].get.operationId")
                .value("getLessonMaterial"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/materials/{materialId}'].patch.operationId")
                .value("updateLessonMaterial"))
            .andExpect(jsonPath("$.components.schemas.CreateLessonMaterialRequest.properties.materialType.enum.length()")
                .value(4))
            .andExpect(jsonPath("$.components.schemas.LessonMaterialResponse.properties.id.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.LessonMaterialResponse.properties.topicId.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.LessonMaterialResponse.properties.createdAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.components.schemas.LessonMaterialResponse.properties.updatedAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/topics/{topicId}/materials'].post.responses['400'].content['*/*'].schema.$ref")
                .value("#/components/schemas/ApiError"));
    }

    private LessonMaterialResult createMaterial(ContentFixture fixture, String title, int position) {
        return lessonMaterialService.createLessonMaterial(
            fixture.principal(),
            new CreateLessonMaterialCommand(
                fixture.topic().id(), LessonMaterialType.TEXT, title, "Содержимое",
                null, null, position
            )
        );
    }

    private String createRequest(
        String materialType,
        String title,
        String content,
        String externalUrl,
        int position
    ) throws Exception {
        return objectMapper.writeValueAsString(new MaterialRequestBody(
            materialType, title, content, externalUrl, position, null
        ));
    }

    private String updateTextRequest(LessonMaterialResult material) throws Exception {
        return objectMapper.writeValueAsString(new MaterialRequestBody(
            "TEXT", material.title(), material.content(), null, material.position(), material.version()
        ));
    }

    private ContentFixture createFixture(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), user, "Teacher"
        ));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Предмет " + UUID.randomUUID(),
            null, SubjectStatus.ACTIVE
        ));
        LearningProgramEntity learningProgram = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Программа", null,
            LearningProgramStatus.DRAFT
        ));
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), learningProgram.getId(), "Модуль", null, 0
        ));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), "Тема", null, 0, TopicStatus.DRAFT
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(),
            email,
            "password-hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
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
        AuthenticatedUser principal,
        TeacherEntity teacher,
        TopicEntity topic
    ) {
    }

    private record MaterialRequestBody(
        String materialType,
        String title,
        String content,
        String externalUrl,
        int position,
        Long version
    ) {
    }
}
