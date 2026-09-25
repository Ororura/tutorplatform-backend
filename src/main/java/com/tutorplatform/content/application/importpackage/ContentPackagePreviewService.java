package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Material;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Module;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewResult.Topic;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ContentPackagePreviewService {
    private static final int MAX_BYTES = 1_048_576;
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
        requireEditable(principal, programId);
        return parseAndPreview(programId, yamlBytes);
    }

    private void requireEditable(AuthenticatedUser principal, UUID programId) {
        var program = programService.get(principal, programId);
        if (!program.editable()) {
            throw new InvalidLearningProgramStatusException(
                    program.hasAssignments()
                            ? "Assigned learning program cannot be edited"
                            : "Archived learning program cannot be edited");
        }
    }

    private ContentPackagePreviewResult parseAndPreview(UUID programId, byte[] yamlBytes) {
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

    public ContentPackagePreviewResult preview(
            AuthenticatedUser principal, UUID programId, MultipartFile file) {
        requireEditable(principal, programId);
        String filename = file.getOriginalFilename();
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".yaml") && !normalized.endsWith(".yml")) {
            throw new ContentPackageParseException(
                    ContentPackageParseException.Code.INVALID_FILE_EXTENSION,
                    "file",
                    "File must have a .yaml or .yml extension");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ContentPackageParseException(
                    ContentPackageParseException.Code.FILE_TOO_LARGE,
                    "file",
                    "YAML file exceeds 1 MiB");
        }
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new ContentPackageParseException(
                        ContentPackageParseException.Code.FILE_TOO_LARGE,
                        "file",
                        "YAML file exceeds 1 MiB");
            }
            return parseAndPreview(programId, bytes);
        } catch (IOException exception) {
            throw new ContentPackageParseException(
                    ContentPackageParseException.Code.FILE_READ_ERROR,
                    "file",
                    "Uploaded file could not be read");
        }
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
