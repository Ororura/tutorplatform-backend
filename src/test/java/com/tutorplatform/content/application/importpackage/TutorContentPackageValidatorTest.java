package com.tutorplatform.content.application.importpackage;

import static com.tutorplatform.content.application.importpackage.ContentPackageValidationError.Code.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.tutorplatform.content.infrastructure.yaml.SnakeYamlContentPackageParser;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TutorContentPackageValidatorTest {
    private final TutorContentPackageValidator validator = new TutorContentPackageValidator();

    @Test
    void acceptsSupportedSchemaVersion() {
        var result = validator.validate(packageWith(new TopicImport("T", null, List.of())));
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertOnlyError(
                new TutorContentPackage(2, "modules", validModules()),
                UNSUPPORTED_SCHEMA_VERSION,
                "schemaVersion");
    }

    @Test
    void requiresSchemaVersion() {
        assertOnlyError(
                new TutorContentPackage(null, "modules", validModules()),
                REQUIRED_FIELD,
                "schemaVersion");
    }

    @Test
    void rejectsWrongKind() {
        assertOnlyError(
                new TutorContentPackage(1, "topics", validModules()), INVALID_PACKAGE_KIND, "kind");
    }

    @Test
    void rejectsEmptyModules() {
        assertOnlyError(
                new TutorContentPackage(1, "modules", List.of()), REQUIRED_FIELD, "modules");
    }

    @Test
    void requiresModuleTitle() {
        assertOnlyError(
                new TutorContentPackage(
                        1, "modules", List.of(new ModuleImport(" ", null, validTopics()))),
                REQUIRED_FIELD,
                "modules[0].title");
    }

    @Test
    void requiresTopicsInModule() {
        assertOnlyError(
                new TutorContentPackage(
                        1, "modules", List.of(new ModuleImport("M", null, List.of()))),
                REQUIRED_FIELD,
                "modules[0].topics");
    }

    @Test
    void requiresTopicTitle() {
        assertOnlyError(
                packageWith(new TopicImport("", null, List.of())),
                REQUIRED_FIELD,
                "modules[0].topics[0].title");
    }

    @Test
    void acceptsTopicWithoutMaterials() {
        var result = validator.validate(packageWith(new TopicImport("T", null, List.of())));
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void requiresMarkdownContent() {
        assertMaterialError(
                new MaterialImport("M", "MARKDOWN", null, null), REQUIRED_FIELD, "content");
    }

    @Test
    void requiresCodeExampleContent() {
        assertMaterialError(
                new MaterialImport("M", "CODE_EXAMPLE", " ", null), REQUIRED_FIELD, "content");
    }

    @Test
    void requiresTextContent() {
        assertMaterialError(new MaterialImport("M", "TEXT", null, null), REQUIRED_FIELD, "content");
    }

    @Test
    void requiresLinkUrl() {
        assertMaterialError(
                new MaterialImport("M", "LINK", null, null), REQUIRED_FIELD, "externalUrl");
    }

    @Test
    void rejectsInvalidLinkUrl() {
        assertMaterialError(
                new MaterialImport("M", "LINK", null, "https:///no-host"),
                INVALID_EXTERNAL_URL,
                "externalUrl");
    }

    @Test
    void rejectsJavascriptLinkUrl() {
        assertMaterialError(
                new MaterialImport("M", "LINK", null, "javascript:alert(1)"),
                INVALID_EXTERNAL_URL,
                "externalUrl");
    }

    @Test
    void rejectsUnknownMaterialTypeWithoutEchoingBody() {
        String secret = "PRIVATE_MATERIAL_BODY";
        var result =
                validator.validate(
                        packageWith(
                                new TopicImport(
                                        "T",
                                        null,
                                        List.of(
                                                new MaterialImport(
                                                        "M", "UNKNOWN", secret, null)))));
        assertThat(result.errors())
                .containsExactly(
                        new ContentPackageValidationError(
                                INVALID_MATERIAL_TYPE,
                                "modules[0].topics[0].materials[0].materialType",
                                "Unsupported material type"));
        assertThat(result.errors().getFirst().message()).doesNotContain(secret, "\tat ");
    }

    @Test
    void rejectsMoreThanFifteenModules() {
        var modules = new ArrayList<ModuleImport>();
        for (int i = 0; i < 16; i++) modules.add(new ModuleImport("M", null, validTopics()));
        var result = validator.validate(new TutorContentPackage(1, "modules", modules));
        assertThat(result.errors())
                .containsExactly(
                        new ContentPackageValidationError(
                                LIMIT_EXCEEDED,
                                "modules",
                                "A package supports at most 15 modules"));
    }

    @Test
    void rejectsMoreThanTwentyFiveTopics() {
        var topics = new ArrayList<TopicImport>();
        for (int i = 0; i < 26; i++) topics.add(new TopicImport("T", null, List.of()));
        var result =
                validator.validate(
                        new TutorContentPackage(
                                1, "modules", List.of(new ModuleImport("M", null, topics))));
        assertThat(result.errors())
                .containsExactly(
                        new ContentPackageValidationError(
                                LIMIT_EXCEEDED,
                                "modules[0].topics",
                                "A module supports at most 25 topics"));
    }

    @Test
    void rejectsMoreThanTwoHundredFiftyMaterials() {
        var materials = new ArrayList<MaterialImport>();
        for (int i = 0; i < 251; i++) materials.add(new MaterialImport("M", "TEXT", "body", null));
        var result = validator.validate(packageWith(new TopicImport("T", null, materials)));
        assertThat(result.errors())
                .containsExactly(
                        new ContentPackageValidationError(
                                LIMIT_EXCEEDED,
                                "modules",
                                "A package supports at most 250 materials"));
    }

    @Test
    void validationDoesNotChangeContentOrCreateLearningEntities() {
        assertThat(TutorContentPackageValidator.class.getDeclaredFields())
                .allSatisfy(field -> assertThat(Modifier.isStatic(field.getModifiers())).isTrue());
        String markdown = "# Heading\n\n  detail\n";
        String python = "if True:\n    print('Привет')\n";
        var materials =
                List.of(
                        new MaterialImport("Notes", "MARKDOWN", markdown, null),
                        new MaterialImport("Code", "CODE_EXAMPLE", python, null));
        var content = packageWith(new TopicImport("T", null, materials));
        assertThat(validator.validate(content).errors()).isEmpty();
        assertThat(content.modules().getFirst().topics().getFirst().materials())
                .containsExactlyElementsOf(materials);
        assertThat(materials.get(0).content()).isEqualTo(markdown);
        assertThat(materials.get(1).content()).isEqualTo(python);
        assertThat(content.modules().getFirst()).isExactlyInstanceOf(ModuleImport.class);
        assertThat(content.modules().getFirst().topics().getFirst())
                .isExactlyInstanceOf(TopicImport.class);
    }

    private static List<ModuleImport> validModules() {
        return List.of(new ModuleImport("M", null, validTopics()));
    }

    private static List<TopicImport> validTopics() {
        return List.of(new TopicImport("T", null, List.of()));
    }

    private void assertOnlyError(
            TutorContentPackage content, ContentPackageValidationError.Code code, String path) {
        var errors = validator.validate(content).errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().code()).isEqualTo(code);
        assertThat(errors.getFirst().path()).isEqualTo(path);
        assertThat(errors.getFirst().message()).doesNotContain("\tat ", "Exception:");
    }

    private void assertMaterialError(
            MaterialImport material, ContentPackageValidationError.Code code, String field) {
        var result = validator.validate(packageWith(new TopicImport("T", null, List.of(material))));
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().code()).isEqualTo(code);
        assertThat(result.errors().getFirst().path())
                .isEqualTo("modules[0].topics[0].materials[0]." + field);
    }

    @Test
    void acceptsDocumentedExampleAndOptionalMaterials() throws IOException {
        var example =
                new SnakeYamlContentPackageParser()
                        .parse(Files.readAllBytes(Path.of("docs/examples/python-conditions.yaml")));
        assertThat(validator.validate(example).valid()).isTrue();
        assertThat(
                        validator
                                .validate(packageWith(new TopicImport("Topic", null, List.of())))
                                .valid())
                .isTrue();
    }

    @Test
    void reportsIndependentErrorsWithPrecisePaths() {
        var content =
                new TutorContentPackage(
                        2,
                        "other",
                        List.of(
                                new ModuleImport(
                                        "  ",
                                        null,
                                        List.of(
                                                new TopicImport(
                                                        null,
                                                        null,
                                                        List.of(
                                                                new MaterialImport(
                                                                        "  ", "IMAGE", null, null),
                                                                new MaterialImport(
                                                                        "Markdown",
                                                                        "MARKDOWN",
                                                                        "  ",
                                                                        null)))))));
        var errors = validator.validate(content).errors();
        assertThat(errors)
                .extracting(ContentPackageValidationError::code)
                .contains(
                        UNSUPPORTED_SCHEMA_VERSION,
                        INVALID_PACKAGE_KIND,
                        REQUIRED_FIELD,
                        INVALID_MATERIAL_TYPE);
        assertThat(errors)
                .extracting(ContentPackageValidationError::path)
                .contains(
                        "schemaVersion",
                        "kind",
                        "modules[0].title",
                        "modules[0].topics[0].title",
                        "modules[0].topics[0].materials[0].title",
                        "modules[0].topics[0].materials[0].materialType",
                        "modules[0].topics[0].materials[1].content");
        assertThat(errors)
                .allSatisfy(error -> assertThat(error.message()).doesNotContain("Markdown"));
    }

    @Test
    void checksRequiredRootAndCollectionsWithoutThrowing() {
        assertError(validator.validate(null), REQUIRED_FIELD, "root");
        var missing = new TutorContentPackage(null, null, null);
        assertError(validator.validate(missing), REQUIRED_FIELD, "schemaVersion");
        assertError(validator.validate(missing), REQUIRED_FIELD, "kind");
        assertError(validator.validate(missing), REQUIRED_FIELD, "modules");
        assertError(
                validator.validate(
                        new TutorContentPackage(
                                1, "modules", List.of(new ModuleImport("M", null, null)))),
                REQUIRED_FIELD,
                "modules[0].topics");
    }

    @Test
    void checksMaterialPayloadAndUrls() {
        var materials =
                List.of(
                        new MaterialImport("Text", "TEXT", "ok", "https://example.org"),
                        new MaterialImport("Link", "LINK", "text", "ftp://example.org"),
                        new MaterialImport("Link", "LINK", null, "/relative"),
                        new MaterialImport("Link", "LINK", null, "https:///missing-host"),
                        new MaterialImport("Link", "LINK", null, "https://example.org/path"),
                        new MaterialImport("Code", "CODE_EXAMPLE", "code", null),
                        new MaterialImport("Bad", null, null, null),
                        new MaterialImport("Link", "LINK", null, null));
        var result = validator.validate(packageWith(new TopicImport("T", null, materials)));
        assertError(result, FORBIDDEN_FIELD, "modules[0].topics[0].materials[0].externalUrl");
        assertError(result, FORBIDDEN_FIELD, "modules[0].topics[0].materials[1].content");
        for (int index : List.of(1, 2, 3)) {
            assertError(
                    result,
                    INVALID_EXTERNAL_URL,
                    "modules[0].topics[0].materials[" + index + "].externalUrl");
        }
        assertError(result, REQUIRED_FIELD, "modules[0].topics[0].materials[6].materialType");
        assertError(result, REQUIRED_FIELD, "modules[0].topics[0].materials[7].externalUrl");
        assertThat(result.errors()).hasSize(7);
    }

    @Test
    void matchesCreateTitleLengthsAndCollectionLimits() {
        var longTopic = new TopicImport("x".repeat(181), null, List.of());
        var result =
                validator.validate(
                        new TutorContentPackage(
                                1,
                                "modules",
                                List.of(
                                        new ModuleImport(
                                                "  " + "m".repeat(180) + "  ",
                                                null,
                                                List.of(longTopic)),
                                        new ModuleImport(
                                                "M",
                                                null,
                                                List.of(
                                                        new TopicImport(
                                                                "T",
                                                                null,
                                                                List.of(
                                                                        new MaterialImport(
                                                                                "x".repeat(201),
                                                                                "TEXT",
                                                                                "body",
                                                                                null))))))));
        assertError(result, LIMIT_EXCEEDED, "modules[0].topics[0].title");
        assertError(result, LIMIT_EXCEEDED, "modules[1].topics[0].materials[0].title");
        assertThat(result.errors()).hasSize(2);

        var manyTopics = new ArrayList<TopicImport>();
        for (int i = 0; i < 26; i++) {
            manyTopics.add(new TopicImport("T", null, List.of()));
        }
        var manyModules = new ArrayList<ModuleImport>();
        for (int i = 0; i < 16; i++) {
            manyModules.add(new ModuleImport("M", null, manyTopics));
        }
        var limits = validator.validate(new TutorContentPackage(1, "modules", manyModules));
        assertError(limits, LIMIT_EXCEEDED, "modules");
        assertError(limits, LIMIT_EXCEEDED, "modules[0].topics");

        var manyMaterials = new ArrayList<MaterialImport>();
        for (int i = 0; i < 251; i++) {
            manyMaterials.add(new MaterialImport("M", "TEXT", "body", null));
        }
        assertError(
                validator.validate(packageWith(new TopicImport("T", null, manyMaterials))),
                LIMIT_EXCEEDED,
                "modules");
    }

    private static TutorContentPackage packageWith(TopicImport topic) {
        return new TutorContentPackage(
                1, "modules", List.of(new ModuleImport("M", null, List.of(topic))));
    }

    private static void assertError(
            ContentPackageValidationResult result,
            ContentPackageValidationError.Code code,
            String path) {
        assertThat(result.errors())
                .anySatisfy(
                        error -> {
                            assertThat(error.code()).isEqualTo(code);
                            assertThat(error.path()).isEqualTo(path);
                        });
    }
}
