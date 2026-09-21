package com.tutorplatform.content.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.exception.*;
import com.tutorplatform.content.api.request.ReorderLessonMaterialsRequest;
import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.user.domain.TeacherRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LessonMaterialService {

    private static final Set<LessonMaterialType> SUPPORTED_TYPES = EnumSet.of(
        LessonMaterialType.MARKDOWN,
        LessonMaterialType.TEXT,
        LessonMaterialType.CODE_EXAMPLE,
        LessonMaterialType.LINK
    );

    private final TeacherRepository teacherRepository;
    private final ProgramQuery programQuery;
    private final LessonMaterialRepository lessonMaterialRepository;

    public LessonMaterialService(
        TeacherRepository teacherRepository,
        ProgramQuery programQuery,
        LessonMaterialRepository lessonMaterialRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.programQuery = programQuery;
        this.lessonMaterialRepository = lessonMaterialRepository;
    }

    @Transactional
    public LessonMaterialResult createLessonMaterial(
        AuthenticatedUser principal,
        CreateLessonMaterialCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedTopic(command.topicId(), teacherId);
        validateMaterial(
            command.materialType(),
            command.title(),
            command.content(),
            command.fileAssetId(),
            command.externalUrl(),
            command.position()
        );

        LessonMaterialEntity material = new LessonMaterialEntity(
            UUID.randomUUID(),
            command.topicId(),
            teacherId,
            command.materialType(),
            command.title(),
            command.content(),
            command.fileAssetId(),
            command.externalUrl(),
            command.position()
        );
        return toResult(saveWithPositionConflict(material));
    }

    @Transactional(readOnly = true)
    public LessonMaterialResult getLessonMaterial(
        AuthenticatedUser principal,
        UUID topicId,
        UUID lessonMaterialId
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedTopicForMaterial(topicId, teacherId);
        return lessonMaterialRepository.findByIdAndTopicId(lessonMaterialId, topicId)
            .map(this::toResult)
            .orElseThrow(LessonMaterialNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<LessonMaterialResult> listLessonMaterials(
        AuthenticatedUser principal,
        UUID topicId
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedTopic(topicId, teacherId);
        return listLessonMaterialsForAuthorizedTopic(topicId);
    }

    @Transactional(readOnly = true)
    public List<LessonMaterialResult> listLessonMaterialsForAuthorizedTopic(UUID topicId) {
        return lessonMaterialRepository.findAllByTopicIdOrderByPosition(topicId).stream()
            .map(this::toResult)
            .toList();
    }

    @Transactional
    public void reorderLessonMaterials(
        AuthenticatedUser principal,
        UUID topicId,
        ReorderLessonMaterialsRequest request
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedTopic(topicId, teacherId);

        List<LessonMaterialEntity> materials = lessonMaterialRepository
            .findAllByTopicIdOrderByPosition(topicId);
        List<UUID> orderedIds = request.orderedIds();
        Set<UUID> existingIds = new HashSet<>(materials.stream().map(LessonMaterialEntity::getId).toList());
        Set<UUID> requestedIds = new HashSet<>(orderedIds);
        if (orderedIds.size() != materials.size()
            || requestedIds.size() != orderedIds.size()
            || !requestedIds.equals(existingIds)) {
            throw new InvalidLessonMaterialOrderException();
        }

        int maxPosition = materials.stream().mapToInt(LessonMaterialEntity::getPosition).max().orElse(-1);
        long highestTemporaryPosition = (long) maxPosition + orderedIds.size();
        if (highestTemporaryPosition > Integer.MAX_VALUE) {
            throw new InvalidLessonMaterialOrderException();
        }

        int temporaryBase = maxPosition + 1;
        for (int index = 0; index < orderedIds.size(); index++) {
            lessonMaterialRepository.updatePosition(topicId, orderedIds.get(index), temporaryBase + index);
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            lessonMaterialRepository.updatePosition(topicId, orderedIds.get(index), index);
        }
    }

    @Transactional
    public LessonMaterialResult updateLessonMaterial(
        AuthenticatedUser principal,
        UUID topicId,
        UUID lessonMaterialId,
        UpdateLessonMaterialCommand command
    ) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedTopicForMaterial(topicId, teacherId);
        LessonMaterialEntity current = lessonMaterialRepository
            .findByIdAndTopicId(lessonMaterialId, topicId)
            .orElseThrow(LessonMaterialNotFoundException::new);
        validateMaterial(
            command.materialType(),
            command.title(),
            command.content(),
            command.fileAssetId(),
            command.externalUrl(),
            command.position()
        );
        if (command.version() < 0) {
            throw new InvalidLessonMaterialException(
                "version", "must be greater than or equal to 0"
            );
        }

        LessonMaterialEntity updated = new LessonMaterialEntity(
            current.getId(),
            current.getTopicId(),
            current.getCreatedByTeacherId(),
            command.materialType(),
            command.title(),
            command.content(),
            command.fileAssetId(),
            command.externalUrl(),
            command.position(),
            command.version(),
            current.getCreatedAt(),
            current.getUpdatedAt()
        );
        return toResult(saveWithPositionConflict(updated));
    }

    private UUID currentTeacherId(AuthenticatedUser principal) {
        return teacherRepository.findByUserId(principal.id()).orElseThrow().id();
    }

    private void requireOwnedTopic(UUID topicId, UUID teacherId) {
        ProgramQuery.TopicContext topic = programQuery.findTopic(topicId)
            .orElseThrow(TopicNotFoundException::new);
        if (!topic.isOwnedBy(teacherId)) {
            throw new TopicNotFoundException();
        }
    }

    private void requireOwnedTopicForMaterial(UUID topicId, UUID teacherId) {
        ProgramQuery.TopicContext topic = programQuery.findTopic(topicId)
            .orElseThrow(LessonMaterialNotFoundException::new);
        if (!topic.isOwnedBy(teacherId)) {
            throw new LessonMaterialNotFoundException();
        }
    }

    private void validateMaterial(
        LessonMaterialType materialType,
        String title,
        String content,
        UUID fileAssetId,
        String externalUrl,
        int position
    ) {
        if (materialType == null || !SUPPORTED_TYPES.contains(materialType)) {
            throw new InvalidLessonMaterialException(
                "materialType", "must be MARKDOWN, TEXT, CODE_EXAMPLE, or LINK"
            );
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new InvalidLessonMaterialException(
                "title", "must contain between 1 and 200 characters"
            );
        }
        if (position < 0) {
            throw new InvalidLessonMaterialException(
                "position", "must be greater than or equal to 0"
            );
        }
        if (isContentType(materialType)) {
            if (content == null || content.isBlank()) {
                throw new InvalidLessonMaterialException("content", "is required");
            }
            if (fileAssetId != null || externalUrl != null) {
                throw new InvalidLessonMaterialException(
                    "material", "text material only supports content"
                );
            }
            return;
        }
        if (externalUrl == null || externalUrl.isBlank()) {
            throw new InvalidLessonMaterialException("externalUrl", "is required");
        }
        if (content != null || fileAssetId != null) {
            throw new InvalidLessonMaterialException(
                "material", "LINK material only supports externalUrl"
            );
        }
    }

    private boolean isContentType(LessonMaterialType materialType) {
        return materialType == LessonMaterialType.MARKDOWN
            || materialType == LessonMaterialType.TEXT
            || materialType == LessonMaterialType.CODE_EXAMPLE;
    }

    private LessonMaterialEntity saveWithPositionConflict(LessonMaterialEntity material) {
        try {
            return lessonMaterialRepository.saveAndFlush(material);
        } catch (DataIntegrityViolationException exception) {
            throw new LessonMaterialPositionConflictException(exception);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new LessonMaterialVersionConflictException(exception);
        }
    }

    private LessonMaterialResult toResult(LessonMaterialEntity material) {
        return new LessonMaterialResult(
            material.getId(),
            material.getTopicId(),
            material.getCreatedByTeacherId(),
            material.getMaterialType(),
            material.getTitle(),
            material.getContent(),
            material.getFileAssetId(),
            material.getExternalUrl(),
            material.getPosition(),
            material.getVersion(),
            material.getCreatedAt(),
            material.getUpdatedAt()
        );
    }
}
