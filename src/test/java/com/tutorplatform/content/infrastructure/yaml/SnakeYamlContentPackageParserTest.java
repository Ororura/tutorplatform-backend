package com.tutorplatform.content.infrastructure.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException.Code;
import com.tutorplatform.content.application.importpackage.TutorContentPackage;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SnakeYamlContentPackageParserTest {
    private final SnakeYamlContentPackageParser parser = new SnakeYamlContentPackageParser();

    @Test
    void parsesValidYamlIntoExactFields() {
        var content =
                parse(
                        "schemaVersion: 1\nkind: modules\nmodules:\n  - title: Intro\n    topics:\n      - title: Basics\n");
        assertThat(content.schemaVersion()).isEqualTo(1);
        assertThat(content.kind()).isEqualTo("modules");
        assertThat(content.modules().getFirst().title()).isEqualTo("Intro");
        assertThat(content.modules().getFirst().topics().getFirst().title()).isEqualTo("Basics");
    }

    @Test
    void parsesSeveralModules() {
        var content =
                parse(
                        "schemaVersion: 1\nkind: modules\nmodules:\n  - title: First\n    topics: []\n  - title: Second\n    topics: []\n");
        assertThat(content.modules()).extracting(m -> m.title()).containsExactly("First", "Second");
    }

    @Test
    void parsesSeveralTopicsInOneModule() {
        var content =
                parse(
                        "modules:\n  - title: M\n    topics:\n      - title: First\n      - title: Second\n");
        assertThat(content.modules().getFirst().topics())
                .extracting(t -> t.title())
                .containsExactly("First", "Second");
    }

    @Test
    void parsesSeveralMaterialsInOneTopic() {
        var content =
                parse(
                        "modules:\n  - title: M\n    topics:\n      - title: T\n        materials:\n          - title: First\n            materialType: TEXT\n            content: one\n          - title: Second\n            materialType: LINK\n            externalUrl: https://example.org\n");
        var materials = content.modules().getFirst().topics().getFirst().materials();
        assertThat(materials).extracting(m -> m.title()).containsExactly("First", "Second");
        assertThat(materials.get(0).content()).isEqualTo("one");
        assertThat(materials.get(1).externalUrl()).isEqualTo("https://example.org");
    }

    @Test
    void preservesOriginalInputAndMarkdownWhitespace() {
        String yaml =
                "modules:\n  - title: M\n    topics:\n      - title: T\n        materials:\n          - title: Notes\n            materialType: MARKDOWN\n            content: |\n              # Heading\n\n                indented **text**\n";
        byte[] bytes = yaml.getBytes(StandardCharsets.UTF_8);
        byte[] original = bytes.clone();
        var content = parser.parse(bytes);
        assertThat(
                        content.modules()
                                .getFirst()
                                .topics()
                                .getFirst()
                                .materials()
                                .getFirst()
                                .content())
                .isEqualTo("# Heading\n\n  indented **text**\n");
        assertThat(bytes).containsExactly(original);
    }

    @Test
    void preservesPythonIndentationAndUnicode() {
        var content =
                parse(
                        "modules:\n  - title: Русский 🐍\n    topics:\n      - title: Условие\n        materials:\n          - title: Код\n            materialType: CODE_EXAMPLE\n            content: |\n              if True:\n                  print(\"Привет\")\n");
        assertThat(content.modules().getFirst().title()).isEqualTo("Русский 🐍");
        assertThat(
                        content.modules()
                                .getFirst()
                                .topics()
                                .getFirst()
                                .materials()
                                .getFirst()
                                .content())
                .isEqualTo("if True:\n    print(\"Привет\")\n");
    }

    @Test
    void rejectsEmptyDocumentWithSafeDiagnostic() {
        assertError("", Code.INVALID_YAML, "root");
        assertThatThrownBy(() -> parse(""))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> {
                            assertThat(ex.getMessage())
                                    .isEqualTo("Exactly one YAML document is required");
                            assertThat(ex.getMessage()).doesNotContain("\tat ", "Exception:");
                        });
    }

    @Test
    void rejectsCustomTagWithoutEchoingMaterial() {
        String secret = "PRIVATE_MATERIAL_BODY";
        assertThatThrownBy(() -> parse("kind: !custom " + secret + "\n"))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> {
                            assertThat(ex.code()).isEqualTo(Code.UNSUPPORTED_YAML_FEATURE);
                            assertThat(ex.location()).startsWith("line ");
                            assertThat(ex.getMessage()).doesNotContain(secret, "\tat ");
                        });
    }

    @Test
    void reportsNestedTypePathWithoutLeakingContent() {
        String secret = "PRIVATE_MATERIAL_BODY";
        assertThatThrownBy(
                        () ->
                                parse(
                                        "modules:\n  - title: M\n    topics:\n      - title: T\n        materials:\n          - title: "
                                                + secret
                                                + "\n            content: false\n"))
                .isInstanceOfSatisfying(
                        ContentPackageParseException.class,
                        ex -> {
                            assertThat(ex.code()).isEqualTo(Code.INVALID_FIELD_TYPE);
                            assertThat(ex.location())
                                    .isEqualTo("modules[0].topics[0].materials[0].content");
                            assertThat(ex.getMessage()).isEqualTo("Expected a string");
                            assertThat(ex.getMessage()).doesNotContain(secret);
                        });
    }

    @Test
    void parsingUsesOnlyInMemoryComponents() {
        assertThat(SnakeYamlContentPackageParser.class.getDeclaredFields())
                .allSatisfy(field -> assertThat(Modifier.isStatic(field.getModifiers())).isTrue());
        var result = parse("schemaVersion: 1\nkind: modules\nmodules: []\n");
        assertThat(result).isExactlyInstanceOf(TutorContentPackage.class);
        assertThat(result.modules()).isEmpty();
    }

    @Test
    void parsesDocumentedExample() throws IOException {
        var content =
                parser.parse(Files.readAllBytes(Path.of("docs/examples/python-conditions.yaml")));
        assertThat(content.schemaVersion()).isEqualTo(1);
        assertThat(content.kind()).isEqualTo("modules");
        assertThat(content.modules()).hasSize(1);
        assertThat(content.modules().getFirst().topics()).hasSize(2);
        assertThat(content.modules().getFirst().topics().getFirst().materials()).hasSize(2);
        assertThat(content.modules().getFirst().topics().getFirst().tasks()).isEmpty();
    }

    @Test
    void parsesV2TextAndCodeTasksWithExactTypesAndOrder() {
        var content =
                parse(
                        """
                schemaVersion: 2
                kind: modules
                modules:
                  - title: Python
                    topics:
                      - title: Conditions
                        tasks:
                          - title: Explain
                            descriptionMarkdown: "Explain **if**."
                            taskType: TEXT
                            difficulty: MEDIUM
                            required: false
                          - title: Check number
                            descriptionMarkdown: |
                              Print the sign.
                            taskType: CODE
                            difficulty: EASY
                            required: true
                            programmingConfig:
                              language: PYTHON
                              starterCode: |
                                value = int(input())
                              executionEnabled: true
                              timeLimitMs: 2000
                              memoryLimitMb: 128
                            testCases:
                              - inputText: "10"
                                expectedOutput: positive
                                hidden: false
                                comparisonMode: NORMALIZED
                              - inputText: "-10"
                                expectedOutput: negative
                                hidden: true
                                comparisonMode: EXACT
                """);
        assertThat(content.schemaVersion()).isEqualTo(2);
        var tasks = content.modules().getFirst().topics().getFirst().tasks();
        assertThat(tasks).extracting(t -> t.title()).containsExactly("Explain", "Check number");
        assertThat(tasks.getFirst().descriptionMarkdown()).isEqualTo("Explain **if**.");
        assertThat(tasks.getFirst().taskType()).isEqualTo("TEXT");
        assertThat(tasks.getFirst().difficulty()).isEqualTo("MEDIUM");
        assertThat(tasks.getFirst().required()).isFalse();
        assertThat(tasks.getFirst().programmingConfig()).isNull();
        assertThat(tasks.getFirst().testCases()).isEmpty();

        var code = tasks.get(1);
        assertThat(code.descriptionMarkdown()).isEqualTo("Print the sign.\n");
        assertThat(code.taskType()).isEqualTo("CODE");
        assertThat(code.difficulty()).isEqualTo("EASY");
        assertThat(code.required()).isTrue();
        assertThat(code.programmingConfig().language()).isEqualTo("PYTHON");
        assertThat(code.programmingConfig().starterCode()).isEqualTo("value = int(input())\n");
        assertThat(code.programmingConfig().executionEnabled()).isTrue();
        assertThat(code.programmingConfig().timeLimitMs()).isEqualTo(2000);
        assertThat(code.programmingConfig().memoryLimitMb()).isEqualTo(128);
        assertThat(code.testCases())
                .extracting(testCase -> testCase.inputText())
                .containsExactly("10", "-10");
        assertThat(code.testCases())
                .extracting(testCase -> testCase.expectedOutput())
                .containsExactly("positive", "negative");
        assertThat(code.testCases().getFirst().hidden()).isFalse();
        assertThat(code.testCases().getFirst().comparisonMode()).isEqualTo("NORMALIZED");
        assertThat(code.testCases().get(1).hidden()).isTrue();
        assertThat(code.testCases().get(1).comparisonMode()).isEqualTo("EXACT");
    }

    @Test
    void doesNotChangeMissingOrFutureSchemaVersions() {
        assertThat(parse("kind: modules\nmodules: []\n").schemaVersion()).isNull();
        assertThat(parse("schemaVersion: 3\nkind: modules\nmodules: []\n").schemaVersion())
                .isEqualTo(3);
    }

    @Test
    void rejectsQuotedAndNonCanonicalBooleans() {
        assertError(
                taskYaml("required: \"true\"\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].required");
        assertError(
                taskYaml("required: yes\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].required");
        assertError(
                taskYaml("programmingConfig:\n  executionEnabled: \"true\"\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].programmingConfig.executionEnabled");
        assertError(
                taskYaml("testCases:\n  - hidden: \"false\"\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].testCases[0].hidden");
    }

    @Test
    void rejectsQuotedIntegersAndOutOfRangeLimits() {
        assertError(
                taskYaml("programmingConfig:\n  timeLimitMs: \"2000\"\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].programmingConfig.timeLimitMs");
        assertError(
                taskYaml("programmingConfig:\n  memoryLimitMb: \"128\"\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].programmingConfig.memoryLimitMb");
        assertError(
                taskYaml("programmingConfig:\n  timeLimitMs: 2147483648\n"),
                Code.INVALID_FIELD_TYPE,
                "modules[0].topics[0].tasks[0].programmingConfig.timeLimitMs");
    }

    @Test
    void rejectsUnknownV2FieldsAtEveryLevel() {
        assertError(
                "modules:\n  - topics:\n      - unexpected: value\n",
                Code.UNKNOWN_FIELD,
                "modules[0].topics[0].unexpected");
        assertError(
                taskYaml("unexpected: value\n"),
                Code.UNKNOWN_FIELD,
                "modules[0].topics[0].tasks[0].unexpected");
        assertError(
                taskYaml("programmingConfig:\n  unexpected: value\n"),
                Code.UNKNOWN_FIELD,
                "modules[0].topics[0].tasks[0].programmingConfig.unexpected");
        assertError(
                taskYaml("testCases:\n  - unexpected: value\n"),
                Code.UNKNOWN_FIELD,
                "modules[0].topics[0].tasks[0].testCases[0].unexpected");
    }

    @Test
    void rejectsDuplicateV2Fields() {
        assertError(
                taskYaml("required: true\nrequired: false\n"),
                Code.DUPLICATE_KEY,
                "modules[0].topics[0].tasks[0].required");
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
        assertThatThrownBy(() -> parse("modules: [[[[[[[[[[x]]]]]]]]]]"))
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

    private String taskYaml(String fields) {
        return """
                schemaVersion: 2
                kind: modules
                modules:
                  - title: M
                    topics:
                      - title: T
                        tasks:
                          - title: Task
                """
                + fields.indent(12);
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
