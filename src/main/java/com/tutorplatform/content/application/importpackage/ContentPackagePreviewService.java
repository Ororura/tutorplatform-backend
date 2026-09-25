package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Material;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Module;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Topic;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentPackagePreviewService {
    private final TeacherLearningProgramService programService;
    private final ContentPackageParser parser;
    private final TutorContentPackageValidator validator;

    public ContentPackagePreviewService(
            TeacherLearningProgramService programService,
            ContentPackageParser parser,
            TutorContentPackageValidator validator) {
        this.programService = programService;
        this.parser = parser;
        this.validator = validator;
    }

    public ContentPackagePreviewResult preview(
            AuthenticatedUser principal, UUID programId, byte[] yamlBytes) {
        var program = programService.get(principal, programId);
        if (!program.editable()) {
            throw new InvalidLearningProgramStatusException(
                    program.hasAssignments()
                            ? "Assigned learning program cannot be edited"
                            : "Archived learning program cannot be edited");
        }

        TutorContentPackage contentPackage = parser.parse(yamlBytes);
        ContentPackageValidationResult validation = validator.validate(contentPackage);
        if (!validation.valid()) {
            throw new ContentPackageValidationException(validation);
        }

        List<Module> modules = contentPackage.modules().stream().map(this::toModule).toList();
        int topicCount = modules.stream().mapToInt(module -> module.topics().size()).sum();
        int materialCount =
                modules.stream()
                        .flatMap(module -> module.topics().stream())
                        .mapToInt(topic -> topic.materials().size())
                        .sum();
        return new ContentPackagePreviewResult(
                programId, sha256(yamlBytes), modules.size(), topicCount, materialCount, modules);
    }

    private Module toModule(ModuleImport module) {
        return new Module(
                module.title().strip(),
                stripNullable(module.description()),
                module.topics().stream().map(this::toTopic).toList());
    }

    private Topic toTopic(TopicImport topic) {
        return new Topic(
                topic.title().strip(),
                stripNullable(topic.description()),
                topic.materials().stream().map(this::toMaterial).toList());
    }

    private Material toMaterial(MaterialImport material) {
        return new Material(
                material.title(),
                LessonMaterialType.valueOf(material.materialType()),
                material.content(),
                material.externalUrl());
    }

    private static String stripNullable(String value) {
        return value == null ? null : value.strip();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
