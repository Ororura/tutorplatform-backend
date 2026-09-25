package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.program.api.CreateLearningProgramModuleRequest;
import com.tutorplatform.program.api.CreateLearningProgramTopicRequest;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentPackageImportService {
    private final TeacherLearningProgramService programs;
    private final ContentPackagePreviewService preview;
    private final LessonMaterialService materials;
    private final ContentPackageImportRepository imports;

    public ContentPackageImportService(
            TeacherLearningProgramService programs,
            ContentPackagePreviewService preview,
            LessonMaterialService materials,
            ContentPackageImportRepository imports) {
        this.programs = programs;
        this.preview = preview;
        this.materials = materials;
        this.imports = imports;
    }

    @Transactional
    public ContentPackageImportResult importPackage(
            AuthenticatedUser principal,
            UUID programId,
            UUID confirmationId,
            String expectedDigest,
            byte[] yamlBytes) {
        UUID teacherId = programs.requireEditableForImport(principal, programId);
        if (confirmationId == null) {
            throw new IllegalArgumentException("confirmationId is required");
        }

        // Preview reparses and validates the bytes and produces the exact values manual create
        // requests must store. It hashes the original bytes, not a serialized package model.
        ContentPackagePreviewResult packagePreview =
                preview.preview(principal, programId, yamlBytes);
        String digest = packagePreview.sha256Digest();
        if (!digest.equals(expectedDigest)) {
            throw new ContentPackageDigestMismatchException();
        }

        var existing = imports.registerConfirmation(teacherId, programId, confirmationId);
        if (existing.isPresent()) {
            if (!digest.equals(existing.get().packageDigest())) {
                throw new ContentPackageConfirmationConflictException();
            }
            return ContentPackageImportResult.fromRecord(existing.get());
        }

        var moduleIds = new ArrayList<UUID>();
        for (var module : packagePreview.modules()) {
            UUID moduleId =
                    programs.createModule(
                                    principal,
                                    programId,
                                    new CreateLearningProgramModuleRequest(
                                            module.title(), module.description()))
                            .id();
            moduleIds.add(moduleId);
            for (var topic : module.topics()) {
                UUID topicId =
                        programs.createTopic(
                                        principal,
                                        programId,
                                        moduleId,
                                        new CreateLearningProgramTopicRequest(
                                                topic.title(), topic.description()))
                                .id();
                for (int position = 0; position < topic.materials().size(); position++) {
                    var material = topic.materials().get(position);
                    materials.createLessonMaterial(
                            principal,
                            new CreateLessonMaterialCommand(
                                    topicId,
                                    material.materialType(),
                                    material.title(),
                                    material.content(),
                                    null,
                                    material.externalUrl(),
                                    position));
                }
            }
        }

        return ContentPackageImportResult.fromRecord(
                imports.saveSuccessfulImport(
                        teacherId,
                        programId,
                        confirmationId,
                        digest,
                        packagePreview.moduleCount(),
                        packagePreview.topicCount(),
                        packagePreview.materialCount(),
                        moduleIds));
    }
}
