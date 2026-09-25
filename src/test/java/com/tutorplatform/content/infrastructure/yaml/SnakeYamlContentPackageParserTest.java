package com.tutorplatform.content.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException.Code;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SnakeYamlContentPackageParserTest {
    private final SnakeYamlContentPackageParser parser = new SnakeYamlContentPackageParser();

    @Test
    void parsesDocumentedExample() throws IOException {
        var content =
                parser.parse(Files.readAllBytes(Path.of("docs/examples/python-conditions.yaml")));
        assertThat(content.schemaVersion()).isEqualTo(1);
        assertThat(content.kind()).isEqualTo("modules");
        assertThat(content.modules()).hasSize(1);
        assertThat(content.modules().getFirst().topics()).hasSize(2);
        assertThat(content.modules().getFirst().topics().getFirst().materials()).hasSize(2);
    }

    @Test
    void preservesOrderAcrossAllCollections() {
        var content =
                parse(
                        """
                schemaVersion: 1
                kind: modules
                modules:
                  - title: First
                    topics:
                      - title: A
                        materials:
                          - title: One
                          - title: Two
                      - title: B
                  - title: Second
                    topics:
                      - title: C
                """);
        assertThat(content.modules()).extracting(m -> m.title()).containsExactly("First", "Second");
        assertThat(content.modules().getFirst().topics())
                .extracting(t -> t.title())
                .containsExactly("A", "B");
        assertThat(content.modules().getFirst().topics().getFirst().materials())
                .extracting(m -> m.title())
                .containsExactly("One", "Two");
    }

    @Test
    void preservesMarkdownCodeAndUnicode() {
        var content =
                parse(
                        """
                schemaVersion: 1
                kind: modules
                modules:
                  - title: "Русский 👋"
                    topics:
                      - title: Тема
                        materials:
                          - title: Markdown
                            content: |
                              # Заголовок
                              **Текст**
                          - title: Code
                            content: |
                              if True:
                                  print("Привет")
                          - title: Folded
                            content: >
                              first
                              second
                """);
        var materials = content.modules().getFirst().topics().getFirst().materials();
        assertThat(content.modules().getFirst().title()).isEqualTo("Русский 👋");
        assertThat(materials.get(0).content()).isEqualTo("# Заголовок\n**Текст**\n");
        assertThat(materials.get(1).content()).isEqualTo("if True:\n    print(\"Привет\")\n");
        assertThat(materials.get(2).content()).isEqualTo("first second\n");
    }

    @Test
    void keepsMissingFieldsAndNormalizesOnlyMaterials() {
        var content =
                parse(
                        """
                schemaVersion: 1
                kind: modules
                modules:
                  - title: M
                    topics:
                      - title: T
                """);
        assertThat(content.modules().getFirst().description()).isNull();
        assertThat(content.modules().getFirst().topics().getFirst().description()).isNull();
        assertThat(content.modules().getFirst().topics().getFirst().materials()).isEmpty();
        assertThat(parse("kind: modules").modules()).isNull();
    }

    @Test
    void rejectsInvalidYaml() {
        assertError("modules: [", Code.INVALID_YAML, "root");
    }

    @Test
    void rejectsDuplicateKeysAtNestedLevel() {
        assertError(
                "modules:\n  - title: A\n    title: B\n", Code.DUPLICATE_KEY, "modules[0].title");
    }

    @Test
    void rejectsInvalidUtf8() {
        assertThatThrownBy(() -> parser.parse(new byte[] {(byte) 0xC3, (byte) 0x28}))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> assertThat(ex.code()).isEqualTo(Code.INVALID_UTF8));
    }

    @Test
    void rejectsOversizeBeforeParsing() {
        byte[] bytes = new byte[1_048_577];
        Arrays.fill(bytes, (byte) 'x');
        assertThatThrownBy(() -> parser.parse(bytes))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> assertThat(ex.code()).isEqualTo(Code.FILE_TOO_LARGE));
    }

    @Test
    void rejectsMultipleDocuments() {
        assertThatThrownBy(() -> parse("---\nkind: modules\n---\nkind: modules\n"))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> assertThat(ex.code()).isEqualTo(Code.INVALID_YAML));
    }

    @Test
    void rejectsAnchors() {
        assertFeature("modules: &x []\n");
    }

    @Test
    void rejectsAliases() {
        assertFeature("modules: &x []\nkind: *x\n");
    }

    @Test
    void rejectsExplicitTags() {
        assertFeature("kind: !!str modules\n");
    }

    @Test
    void rejectsUnknownFields() {
        assertError(
                "modules:\n  - unexpectedField: true\n",
                Code.UNKNOWN_FIELD,
                "modules[0].unexpectedField");
    }

    @Test
    void rejectsMappingInsteadOfString() {
        assertError("modules:\n  - title: {a: b}\n", Code.INVALID_FIELD_TYPE, "modules[0].title");
    }

    @Test
    void rejectsScalarCoercion() {
        assertError("kind: true\n", Code.INVALID_FIELD_TYPE, "kind");
    }

    @Test
    void rejectsExcessiveNesting() {
        assertThatThrownBy(() -> parse("modules: [[[[[[[[x]]]]]]]]"))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> assertThat(ex.code()).isEqualTo(Code.UNSUPPORTED_YAML_FEATURE));
    }

    @Test
    void rejectsNullInsteadOfArray() {
        assertError("modules: null\n", Code.INVALID_FIELD_TYPE, "modules");
    }

    private void assertFeature(String yaml) {
        assertThatThrownBy(() -> parse(yaml))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> assertThat(ex.code()).isEqualTo(Code.UNSUPPORTED_YAML_FEATURE));
    }

    private void assertError(String yaml, Code code, String path) {
        assertThatThrownBy(() -> parse(yaml))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> {
                            assertThat(ex.code()).isEqualTo(code);
                            assertThat(ex.location()).isEqualTo(path);
                        });
    }

    private com.tutorplatform.content.application.importpackage.TutorContentPackage parse(
            String yaml) {
        return parser.parse(yaml.getBytes(StandardCharsets.UTF_8));
    }
}
