package com.tutorplatform.content.application.importpackage;

import static com.tutorplatform.content.application.importpackage.ContentPackageValidationError.Code.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.tutorplatform.content.infrastructure.yaml.SnakeYamlContentPackageParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TutorContentPackageValidatorTest {
    private final TutorContentPackageValidator validator = new TutorContentPackageValidator();

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
