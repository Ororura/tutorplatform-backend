package com.tutorplatform.content.application.importpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.content.infrastructure.yaml.SnakeYamlContentPackageParser;
import com.tutorplatform.program.api.LearningProgramDetailsResponse;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.LearningProgramNotFoundException;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentPackagePreviewServiceTest {
    private final TeacherLearningProgramService programs =
            mock(TeacherLearningProgramService.class);
    private final AuthenticatedUser teacher =
            new AuthenticatedUser(UUID.randomUUID(), "teacher", "", true, List.of());
    private final UUID programId = UUID.randomUUID();

    @Test
    void previewsHierarchyCountsOrderAndNormalizedFieldsWithoutChangingMaterialContent() {
        editableProgram();
        var service =
                new ContentPackagePreviewService(
                        programs,
                        new SnakeYamlContentPackageParser(),
                        new TutorContentPackageValidator());
        byte[] yaml =
                ("schemaVersion: 1\n"
                                + "kind: modules\n"
                                + "modules:\n"
                                + "  - title: ' First module '\n"
                                + "    description: ' Description '\n"
                                + "    topics:\n"
                                + "      - title: ' First topic '\n"
                                + "        description: ' Topic description '\n"
                                + "        materials:\n"
                                + "          - title: ' Notes '\n"
                                + "            materialType: MARKDOWN\n"
                                + "            content: |\n"
                                + "              # Heading\n"
                                + "\n"
                                + "                indented **text**\n"
                                + "          - title: Code\n"
                                + "            materialType: CODE_EXAMPLE\n"
                                + "            content: |\n"
                                + "              if True:\n"
                                + "                  print('yes')\n"
                                + "      - title: Second topic\n"
                                + "  - title: Second module\n"
                                + "    topics:\n"
                                + "      - title: Third topic\n"
                                + "        materials:\n"
                                + "          - title: Reference\n"
                                + "            materialType: LINK\n"
                                + "            externalUrl: https://example.org/reference\n")
                        .getBytes(StandardCharsets.UTF_8);

        var result = service.preview(teacher, programId, yaml);

        assertThat(result.programId()).isEqualTo(programId);
        assertThat(result.moduleCount()).isEqualTo(2);
        assertThat(result.topicCount()).isEqualTo(3);
        assertThat(result.materialCount()).isEqualTo(3);
        assertThat(result.modules())
                .extracting(ContentPackagePreviewResult.Module::title)
                .containsExactly("First module", "Second module");
        var first = result.modules().getFirst();
        assertThat(first.description()).isEqualTo("Description");
        assertThat(first.topics())
                .extracting(ContentPackagePreviewResult.Topic::title)
                .containsExactly("First topic", "Second topic");
        assertThat(first.topics().getFirst().description()).isEqualTo("Topic description");
        var materials = first.topics().getFirst().materials();
        assertThat(materials)
                .extracting(ContentPackagePreviewResult.Material::title)
                .containsExactly(" Notes ", "Code");
        assertThat(materials.getFirst().materialType()).isEqualTo(LessonMaterialType.MARKDOWN);
        assertThat(materials.getFirst().content()).isEqualTo("# Heading\n\n  indented **text**\n");
        assertThat(materials.get(1).content()).isEqualTo("if True:\n    print('yes')\n");
        assertThat(result.modules().get(1).topics().getFirst().materials().getFirst().externalUrl())
                .isEqualTo("https://example.org/reference");
    }

    @Test
    void hashesOriginalBytes() {
        editableProgram();
        var parser = mock(ContentPackageParser.class);
        when(parser.parse(new byte[] {'a', 'b', 'c'}))
                .thenReturn(
                        new TutorContentPackage(
                                1,
                                "modules",
                                List.of(
                                        new ModuleImport(
                                                "M",
                                                null,
                                                List.of(new TopicImport("T", null, List.of()))))));
        var service =
                new ContentPackagePreviewService(
                        programs, parser, new TutorContentPackageValidator());

        assertThat(service.preview(teacher, programId, new byte[] {'a', 'b', 'c'}).sha256Digest())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void propagatesParserErrorWithoutFileContents() {
        editableProgram();
        var parser = mock(ContentPackageParser.class);
        var error =
                new ContentPackageParseException(
                        ContentPackageParseException.Code.INVALID_YAML, "root", "Invalid YAML");
        byte[] bytes = "secret file content".getBytes(StandardCharsets.UTF_8);
        when(parser.parse(bytes)).thenThrow(error);
        var service =
                new ContentPackagePreviewService(
                        programs, parser, new TutorContentPackageValidator());

        assertThatThrownBy(() -> service.preview(teacher, programId, bytes)).isSameAs(error);
    }

    @Test
    void reportsValidatorErrorsWithCodePathAndMessage() {
        editableProgram();
        var service =
                new ContentPackagePreviewService(
                        programs,
                        new SnakeYamlContentPackageParser(),
                        new TutorContentPackageValidator());
        byte[] yaml =
                "schemaVersion: 1\nkind: modules\nmodules:\n  - title: Module\n    topics:\n      - title: Topic\n        materials:\n          - title: Bad\n            materialType: MARKDOWN\n"
                        .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.preview(teacher, programId, yaml))
                .isInstanceOfSatisfying(
                        ContentPackageValidationException.class,
                        error -> {
                            assertThat(error.result().valid()).isFalse();
                            assertThat(error.result().errors())
                                    .containsExactly(
                                            new ContentPackageValidationError(
                                                    ContentPackageValidationError.Code
                                                            .REQUIRED_FIELD,
                                                    "modules[0].topics[0].materials[0].content",
                                                    "Content is required for MARKDOWN material"));
                        });
    }

    @Test
    void rejectsAnotherTeachersProgramBeforeParsing() {
        var parser = mock(ContentPackageParser.class);
        when(programs.get(teacher, programId)).thenThrow(new LearningProgramNotFoundException());
        var service =
                new ContentPackagePreviewService(
                        programs, parser, new TutorContentPackageValidator());

        assertThatThrownBy(() -> service.preview(teacher, programId, new byte[0]))
                .isInstanceOf(LearningProgramNotFoundException.class);
        verifyNoInteractions(parser);
    }

    @Test
    void rejectsArchivedAndAssignedProgramsBeforeParsing() {
        var parser = mock(ContentPackageParser.class);
        var service =
                new ContentPackagePreviewService(
                        programs, parser, new TutorContentPackageValidator());
        for (boolean assigned : List.of(false, true)) {
            var details = mock(LearningProgramDetailsResponse.class);
            when(details.editable()).thenReturn(false);
            when(details.hasAssignments()).thenReturn(assigned);
            when(programs.get(teacher, programId)).thenReturn(details);

            assertThatThrownBy(() -> service.preview(teacher, programId, new byte[0]))
                    .isInstanceOf(InvalidLearningProgramStatusException.class);
        }
        verifyNoInteractions(parser);
    }

    private void editableProgram() {
        var details = mock(LearningProgramDetailsResponse.class);
        when(details.editable()).thenReturn(true);
        when(programs.get(teacher, programId)).thenReturn(details);
    }
}
