package com.tutorplatform.content.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.application.exception.LessonMaterialNotFoundException;
import com.tutorplatform.content.application.exception.LessonMaterialPositionConflictException;
import com.tutorplatform.content.application.exception.TopicNotFoundException;
import com.tutorplatform.content.domain.*;
import com.tutorplatform.file.application.FileStorage;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.user.domain.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileMaterialService {
    private static final Logger log = LoggerFactory.getLogger(FileMaterialService.class);
    private final TeacherRepository teachers;
    private final ProgramQuery programs;
    private final LessonMaterialRepository materials;
    private final FileAssetRepository assets;
    private final FileStorage storage;
    private final FileMaterialPolicy policy;
    private final LessonMaterialService materialService;

    public FileMaterialService(TeacherRepository teachers, ProgramQuery programs,
                               LessonMaterialRepository materials, FileAssetRepository assets, FileStorage storage,
                               FileMaterialPolicy policy, LessonMaterialService materialService) {
        this.teachers = teachers;
        this.programs = programs;
        this.materials = materials;
        this.assets = assets;
        this.storage = storage;
        this.policy = policy;
        this.materialService = materialService;
    }

    @Transactional
    public LessonMaterialResult upload(AuthenticatedUser principal, UUID topicId,
                                       LessonMaterialType type, String title, int position, String filename,
                                       String mimeType, long size, InputStream input) {
        UUID teacherId = teachers.findByUserId(principal.id()).orElseThrow().id();
        if (!programs.findTopic(topicId).orElseThrow(TopicNotFoundException::new).isOwnedBy(teacherId)) {
            throw new TopicNotFoundException();
        }
        if (type != LessonMaterialType.FILE && type != LessonMaterialType.IMAGE) {
            throw new InvalidLessonMaterialException("materialType", "must be FILE or IMAGE");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new InvalidLessonMaterialException("title", "must contain between 1 and 200 characters");
        }
        if (position < 0) throw new InvalidLessonMaterialException("position", "must be nonnegative");
        if (filename == null || filename.isBlank() || filename.length() > 255
            || filename.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidLessonMaterialException("file", "invalid original filename");
        }
        byte[] content = policy.readAndValidate(input, size, mimeType, type);
        var object = storage.store(content);
        // afterCompletion includes failures at COMMIT, unlike a catch around repository.save().
        // MVP: rollback deletes the object; failed cleanup/unknown outcome is an actionable
        // ERROR with an opaque key for reconciliation. Process-crash atomicity needs an outbox/staging protocol.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        storage.delete(object.key());
                    } catch (RuntimeException cleanup) {
                        log.error(
                            "FILE_STORAGE_CLEANUP_REQUIRED provider={} key={}",
                            object.provider(),
                            object.key(),
                            cleanup
                        );
                    }
                } else if (status != STATUS_COMMITTED) {
                    log.error(
                        "FILE_STORAGE_RECONCILIATION_REQUIRED provider={} key={}",
                        object.provider(),
                        object.key()
                    );
                }
            }
        });
        FileAssetEntity asset = assets.saveAndFlush(new FileAssetEntity(
            UUID.randomUUID(),
            teacherId,
            StorageProvider.valueOf(object.provider()),
            object.key(),
            filename,
            mimeType,
            content.length,
            sha256(content)
        ));
        LessonMaterialEntity material;
        try {
            material = materials.saveAndFlush(new LessonMaterialEntity(UUID.randomUUID(), topicId,
                teacherId, type, title, null, asset.id(), null, position));
        } catch (DataIntegrityViolationException exception) {
            throw new LessonMaterialPositionConflictException(exception);
        }
        return materialService.getLessonMaterial(principal, topicId, material.getId());
    }

    @Transactional(readOnly = true)
    public Download download(AuthenticatedUser principal, UUID topicId, UUID materialId) {
        LessonMaterialResult material = materialService.getLessonMaterial(principal, topicId, materialId);
        if (material.fileAssetId() == null) {
            throw new LessonMaterialNotFoundException();
        }
        FileAssetEntity asset = assets.findById(material.fileAssetId())
            .orElseThrow(LessonMaterialNotFoundException::new);
        return new Download(asset.originalFilename(), asset.mimeType(),
            storage.read(asset.storageKey(), asset.sizeBytes()));
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Download(String filename, String mimeType, byte[] content) {
    }
}
