package com.tutorplatform.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OpenApiHardeningIntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of(
        "get", "post", "put", "patch", "delete", "head", "options", "trace"
    );
    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
        "getCsrfToken", "registerTeacher", "login", "logout",
        "getPublicStudentInvitation", "acceptStudentInvitation",
        "getPublicCurrentProgress", "getPublicProgressReport",
        "downloadPublicProgressReportPdf"
    );
    private static final Set<String> SERVER_OWNED_REQUEST_FIELDS = Set.of(
        "teacherId", "generatedByTeacherId", "ownerTeacherId", "userId", "studentId",
        "relationType", "createdAt", "updatedAt", "publishedAt", "revokedAt",
        "acceptedAt", "tokenHash", "attemptNo", "snapshot", "learningMinutes",
        "executionStatus", "passedTests", "totalTests", "executionTimeMs",
        "stdoutExcerpt", "stderrExcerpt"
    );
    private static final Set<String> INTERNAL_CONTRACT_FIELDS = Set.of(
        "passwordHash", "sessionId", "tokenHash", "storageKey"
    );

    @Container
    private static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void generatedContractHasUniqueOperationIdsAndExactCookieSecurity() throws Exception {
        JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray());

        JsonNode cookieSession = document.required("components").required("securitySchemes")
            .required("cookieSession");
        assertThat(cookieSession.required("type").asText()).isEqualTo("apiKey");
        assertThat(cookieSession.required("in").asText()).isEqualTo("cookie");
        assertThat(cookieSession.required("name").asText()).isEqualTo("TUTOR_SESSION");
        assertThat(document.required("security").toString()).contains("cookieSession");

        Set<String> operationIds = new HashSet<>();
        Iterator<JsonNode> paths = document.required("paths").elements();
        while (paths.hasNext()) {
            JsonNode path = paths.next();
            Iterator<String> fields = path.fieldNames();
            while (fields.hasNext()) {
                String method = fields.next();
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }
                JsonNode operation = path.required(method);
                String operationId = operation.required("operationId").asText();
                assertThat(operationId).isNotBlank();
                assertThat(operationIds.add(operationId))
                    .as("unique operationId %s", operationId)
                    .isTrue();
                if (PUBLIC_OPERATIONS.contains(operationId)) {
                    assertThat(operation.required("security").isArray()).isTrue();
                    assertThat(operation.required("security").isEmpty()).isTrue();
                } else {
                    assertThat(operation.has("security")).isFalse();
                }
            }
        }
        assertThat(operationIds).hasSize(63);

        JsonNode publicPdf = document.required("paths")
            .required("/api/v1/public/reports/{token}/pdf")
            .required("get").required("responses").required("200")
            .required("content").required("application/pdf").required("schema");
        assertThat(publicPdf.required("type").asText()).isEqualTo("string");
        assertThat(publicPdf.required("format").asText()).isEqualTo("binary");
    }

    @Test
    void requestSchemasDoNotExposeServerOwnedAssignmentFields() throws Exception {
        JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray());
        JsonNode schemas = document.required("components").required("schemas");

        schemas.fields().forEachRemaining(schema -> {
            JsonNode properties = schema.getValue().path("properties");
            INTERNAL_CONTRACT_FIELDS.forEach(field -> assertThat(properties.has(field))
                .as("%s must not export internal field %s", schema.getKey(), field)
                .isFalse());
            if (!schema.getKey().endsWith("Request")) {
                return;
            }
            SERVER_OWNED_REQUEST_FIELDS.forEach(field -> assertThat(properties.has(field))
                .as("%s must not accept server-owned field %s", schema.getKey(), field)
                .isFalse());
        });
    }

    @Test
    void componentReferencesAndCommonFormatsResolveCleanly() throws Exception {
        JsonNode document = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray());
        JsonNode schemas = document.required("components").required("schemas");

        assertReferencesResolve(document, schemas);
        schemas.fields().forEachRemaining(schema -> {
            JsonNode value = schema.getValue();
            if ("string".equals(value.path("type").asText())) {
                assertThat(value.path("enum").isArray() && !value.path("enum").isEmpty())
                    .as("string component %s declares its enum values", schema.getKey())
                    .isTrue();
            }
            value.path("properties").fields().forEachRemaining(property -> {
                String name = property.getKey();
                JsonNode definition = property.getValue();
                if ("string".equals(definition.path("type").asText())
                    && !name.equals("traceId")
                    && (name.equals("id") || name.endsWith("Id"))) {
                    assertThat(definition.path("format").asText())
                        .as("%s.%s UUID format", schema.getKey(), name)
                        .isEqualTo("uuid");
                }
                if ("string".equals(definition.path("type").asText()) && name.endsWith("At")) {
                    assertThat(definition.path("format").asText())
                        .as("%s.%s date-time format", schema.getKey(), name)
                        .isEqualTo("date-time");
                }
            });
        });
    }

    private static void assertReferencesResolve(JsonNode node, JsonNode schemas) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                if (field.getKey().equals("$ref")) {
                    String reference = field.getValue().asText();
                    String prefix = "#/components/schemas/";
                    if (reference.startsWith(prefix)) {
                        assertThat(schemas.has(reference.substring(prefix.length())))
                            .as("resolved schema reference %s", reference)
                            .isTrue();
                    }
                } else {
                    assertReferencesResolve(field.getValue(), schemas);
                }
            });
        } else if (node.isArray()) {
            node.forEach(item -> assertReferencesResolve(item, schemas));
        }
    }
}
