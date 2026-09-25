package com.tutorplatform.content.application.importpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.api.CreateLearningProgramModuleRequest;
import com.tutorplatform.program.api.CreateLearningProgramTopicRequest;
import com.tutorplatform.program.api.LearningProgramModuleResponse;
import com.tutorplatform.program.api.LearningProgramTopicDetailsResponse;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.LearningProgramNotFoundException;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentPackageImportServiceTest {
    private static final String DIGEST = "a".repeat(64);
    private final AuthenticatedUser principal =
            new AuthenticatedUser(UUID.randomUUID(), "teacher", "", true, List.of());
    private final UUID teacherId = UUID.randomUUID();
    private final UUID programId = UUID.randomUUID();
    private final UUID confirmationId = UUID.randomUUID();
    private final byte[] yamlBytes = new byte[] {1, 2, 3};
    private final TeacherLearningProgramService programs =
            mock(TeacherLearningProgramService.class);
    private final ContentPackagePreviewService preview = mock(ContentPackagePreviewService.class);
    private final LessonMaterialService materials = mock(LessonMaterialService.class);
    private final ContentPackageImportRepository imports =
            mock(ContentPackageImportRepository.class);
    private final ContentPackageImportService service =
            new ContentPackageImportService(programs, preview, materials, imports);

    @Test
    void importsInYamlOrderWithNormalizedTitlesAndUntouchedPayloads() {
        UUID firstModule = UUID.randomUUID();
        UUID secondModule = UUID.randomUUID();
        UUID firstTopic = UUID.randomUUID();
        UUID secondTopic = UUID.randomUUID();
        ready(
                new ContentPackagePreviewResult(
                        programId,
                        DIGEST,
                        2,
                        2,
                        3,
                        List.of(
                                new ContentPackagePreviewResult.Module(
                                        "First",
                                        "Description",
                                        List.of(
                                                new ContentPackagePreviewResult.Topic(
                                                        "Topic A",
                                                        "Topic description",
                                                        List.of(
                                                                new ContentPackagePreviewResult
                                                                        .Material(
                                                                        " Markdown ",
                                                                        LessonMaterialType.MARKDOWN,
                                                                        "# Heading\n  indented\n",
                                                                        null),
                                                                new ContentPackagePreviewResult
                                                                        .Material(
                                                                        "Code",
                                                                        LessonMaterialType
                                                                                .CODE_EXAMPLE,
                                                                        "if True:\n    print('yes')\n",
                                                                        null))))),
                                new ContentPackagePreviewResult.Module(
                                        "Second",
                                        null,
                                        List.of(
                                                new ContentPackagePreviewResult.Topic(
                                                        "Topic B",
                                                        null,
                                                        List.of(
                                                                new ContentPackagePreviewResult
                                                                        .Material(
                                                                        "Link",
                                                                        LessonMaterialType.LINK,
                                                                        null,
                                                                        "https://example.org/a"))))))));
        when(programs.createModule(eq(principal), eq(programId), any()))
                .thenReturn(
                        new LearningProgramModuleResponse(firstModule, "First", "Description", 4),
                        new LearningProgramModuleResponse(secondModule, "Second", null, 5));
        when(programs.createTopic(eq(principal), eq(programId), any(), any()))
                .thenReturn(
                        new LearningProgramTopicDetailsResponse(
                                firstTopic, "a", "Topic A", "Topic description", 0, null, 0L),
                        new LearningProgramTopicDetailsResponse(
                                secondTopic, "b", "Topic B", null, 0, null, 0L));
        when(imports.saveSuccessfulImport(
                        teacherId,
                        programId,
                        confirmationId,
                        DIGEST,
                        2,
                        2,
                        3,
                        List.of(firstModule, secondModule)))
                .thenReturn(record(DIGEST, List.of(firstModule, secondModule), 2, 2, 3));

        var result = service.importPackage(principal, programId, confirmationId, DIGEST, yamlBytes);

        assertThat(result.createdModuleIds()).containsExactly(firstModule, secondModule);
        assertThat(result.moduleCount()).isEqualTo(2);
        assertThat(result.topicCount()).isEqualTo(2);
        assertThat(result.materialCount()).isEqualTo(3);
        var order = inOrder(programs, preview, imports, materials);
        order.verify(programs).requireEditableForImport(principal, programId);
        order.verify(preview).preview(principal, programId, yamlBytes);
        order.verify(imports).registerConfirmation(teacherId, programId, confirmationId);
        order.verify(programs)
                .createModule(
                        principal,
                        programId,
                        new CreateLearningProgramModuleRequest("First", "Description"));
        order.verify(programs)
                .createTopic(
                        principal,
                        programId,
                        firstModule,
                        new CreateLearningProgramTopicRequest("Topic A", "Topic description"));
        order.verify(materials)
                .createLessonMaterial(
                        principal,
                        new CreateLessonMaterialCommand(
                                firstTopic,
                                LessonMaterialType.MARKDOWN,
                                " Markdown ",
                                "# Heading\n  indented\n",
                                null,
                                null,
                                0));
        order.verify(materials)
                .createLessonMaterial(
                        principal,
                        new CreateLessonMaterialCommand(
                                firstTopic,
                                LessonMaterialType.CODE_EXAMPLE,
                                "Code",
                                "if True:\n    print('yes')\n",
                                null,
                                null,
                                1));
        order.verify(programs)
                .createModule(
                        principal,
                        programId,
                        new CreateLearningProgramModuleRequest("Second", null));
        order.verify(programs)
                .createTopic(
                        principal,
                        programId,
                        secondModule,
                        new CreateLearningProgramTopicRequest("Topic B", null));
        order.verify(materials)
                .createLessonMaterial(
                        principal,
                        new CreateLessonMaterialCommand(
                                secondTopic,
                                LessonMaterialType.LINK,
                                "Link",
                                null,
                                null,
                                "https://example.org/a",
                                0));
        order.verify(imports)
                .saveSuccessfulImport(
                        teacherId,
                        programId,
                        confirmationId,
                        DIGEST,
                        2,
                        2,
                        3,
                        List.of(firstModule, secondModule));
    }

    @Test
    void rejectsChangedBytesBeforeRegisteringConfirmation() {
        ready(packagePreview());
        assertThatThrownBy(
                        () ->
                                service.importPackage(
                                        principal,
                                        programId,
                                        confirmationId,
                                        "b".repeat(64),
                                        yamlBytes))
                .isInstanceOf(ContentPackageDigestMismatchException.class);
        verifyNoInteractions(imports, materials);
    }

    @Test
    void replaysExistingConfirmationWithoutCreatingContent() {
        ready(packagePreview());
        UUID moduleId = UUID.randomUUID();
        when(imports.registerConfirmation(teacherId, programId, confirmationId))
                .thenReturn(Optional.of(record(DIGEST, List.of(moduleId), 1, 1, 0)));
        assertThat(service.importPackage(principal, programId, confirmationId, DIGEST, yamlBytes))
                .isEqualTo(
                        ContentPackageImportResult.fromRecord(
                                record(DIGEST, List.of(moduleId), 1, 1, 0)));
        verifyNoInteractions(materials);
    }

    @Test
    void rejectsConfirmationAlreadyUsedWithAnotherDigest() {
        ready(packagePreview());
        when(imports.registerConfirmation(teacherId, programId, confirmationId))
                .thenReturn(
                        Optional.of(record("b".repeat(64), List.of(UUID.randomUUID()), 1, 1, 0)));
        assertThatThrownBy(
                        () ->
                                service.importPackage(
                                        principal, programId, confirmationId, DIGEST, yamlBytes))
                .isInstanceOf(ContentPackageConfirmationConflictException.class);
        verifyNoInteractions(materials);
    }

    @Test
    void rejectsMissingOrUneditableProgramBeforeParsing() {
        when(programs.requireEditableForImport(principal, programId))
                .thenThrow(new LearningProgramNotFoundException())
                .thenThrow(new InvalidLearningProgramStatusException("Archived"));
        assertThatThrownBy(
                        () ->
                                service.importPackage(
                                        principal, programId, confirmationId, DIGEST, yamlBytes))
                .isInstanceOf(LearningProgramNotFoundException.class);
        assertThatThrownBy(
                        () ->
                                service.importPackage(
                                        principal, programId, confirmationId, DIGEST, yamlBytes))
                .isInstanceOf(InvalidLearningProgramStatusException.class);
        verifyNoInteractions(preview, imports, materials);
    }

    private void ready(ContentPackagePreviewResult result) {
        when(programs.requireEditableForImport(principal, programId)).thenReturn(teacherId);
        when(preview.preview(principal, programId, yamlBytes)).thenReturn(result);
    }

    private ContentPackagePreviewResult packagePreview() {
        return new ContentPackagePreviewResult(
                programId,
                DIGEST,
                1,
                1,
                0,
                List.of(
                        new ContentPackagePreviewResult.Module(
                                "Module",
                                null,
                                List.of(
                                        new ContentPackagePreviewResult.Topic(
                                                "Topic", null, List.of())))));
    }

    private ContentPackageImportRecord record(
            String digest, List<UUID> ids, int moduleCount, int topicCount, int materialCount) {
        return new ContentPackageImportRecord(
                UUID.randomUUID(),
                teacherId,
                programId,
                confirmationId,
                digest,
                moduleCount,
                topicCount,
                materialCount,
                ids,
                Instant.now());
    }
}
