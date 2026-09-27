package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import com.tutorplatform.program.api.LearningProgramSummaryResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LearningProgramDuplicationService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final String COPY_SUFFIX = " — копия";

    private final StudentOwnershipQuery ownershipQuery;
    private final LearningProgramRepository learningProgramRepository;
    private final ModuleRepository moduleRepository;
    private final TopicRepository topicRepository;
    private final LessonMaterialRepository lessonMaterialRepository;
    private final TopicTaskRepository topicTaskRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherLearningProgramQuery programQuery;

    public LearningProgramDuplicationService(
            StudentOwnershipQuery ownershipQuery,
            LearningProgramRepository learningProgramRepository,
            ModuleRepository moduleRepository,
            TopicRepository topicRepository,
            LessonMaterialRepository lessonMaterialRepository,
            TopicTaskRepository topicTaskRepository,
            SubjectRepository subjectRepository,
            TeacherLearningProgramQuery programQuery) {
        this.ownershipQuery = ownershipQuery;
        this.learningProgramRepository = learningProgramRepository;
        this.moduleRepository = moduleRepository;
        this.topicRepository = topicRepository;
        this.lessonMaterialRepository = lessonMaterialRepository;
        this.topicTaskRepository = topicTaskRepository;
        this.subjectRepository = subjectRepository;
        this.programQuery = programQuery;
    }

    @Transactional
    public LearningProgramSummaryResponse duplicate(
            AuthenticatedUser principal, UUID sourceProgramId) {
        UUID teacherId = teacherId(principal);

        LearningProgramEntity source =
                learningProgramRepository
                        .findById(sourceProgramId)
                        .filter(program -> program.getTeacherId().equals(teacherId))
                        .orElseThrow(LearningProgramNotFoundException::new);

        SubjectEntity subject =
                subjectRepository
                        .findById(source.getSubjectId())
                        .filter(
                                candidate ->
                                        candidate.ownerTeacherId() == null
                                                || candidate.ownerTeacherId().equals(teacherId))
                        .filter(candidate -> candidate.status() == SubjectStatus.ACTIVE)
                        .orElseThrow(SubjectNotFoundException::new);

        LearningProgramEntity copy =
                learningProgramRepository.saveAndFlush(
                        new LearningProgramEntity(
                                UUID.randomUUID(),
                                teacherId,
                                source.getSubjectId(),
                                copyTitle(source.getTitle()),
                                source.getDescription(),
                                LearningProgramStatus.DRAFT));

        moduleRepository.findByLearningProgramId(sourceProgramId).stream()
                .sorted(Comparator.comparingInt(ModuleEntity::position))
                .forEach(module -> duplicateModule(teacherId, module, copy.getId()));

        return new LearningProgramSummaryResponse(
                copy.getId(),
                programQuery
                        .findSlug(teacherId, copy.getId())
                        .orElseThrow(LearningProgramNotFoundException::new),
                new ProgramSubjectResponse(subject.id(), subject.code(), subject.name()),
                copy.getTitle(),
                copy.getDescription(),
                copy.getStatus(),
                copy.getCreatedAt(),
                copy.getUpdatedAt());
    }

    private void duplicateModule(UUID teacherId, ModuleEntity source, UUID targetProgramId) {
        ModuleEntity copy =
                moduleRepository.saveAndFlush(
                        new ModuleEntity(
                                UUID.randomUUID(),
                                targetProgramId,
                                source.title(),
                                source.description(),
                                source.position()));

        topicRepository.findByModuleId(source.id()).stream()
                .sorted(Comparator.comparingInt(TopicEntity::position))
                .forEach(topic -> duplicateTopic(teacherId, topic, copy.id()));
    }

    private void duplicateTopic(UUID teacherId, TopicEntity source, UUID targetModuleId) {
        TopicEntity copy =
                topicRepository.saveAndFlush(
                        new TopicEntity(
                                UUID.randomUUID(),
                                targetModuleId,
                                source.title(),
                                source.description(),
                                source.position(),
                                source.status()));

        lessonMaterialRepository.findAllByTopicIdOrderByPosition(source.id()).stream()
                .forEach(material -> duplicateMaterial(teacherId, material, copy.id()));

        topicTaskRepository.findAllByTopicIdOrderByPosition(source.id()).stream()
                .forEach(
                        link ->
                                topicTaskRepository.saveAndFlush(
                                        new TopicTaskEntity(
                                                copy.id(),
                                                link.taskId(),
                                                link.position(),
                                                link.required())));
    }

    private void duplicateMaterial(
            UUID teacherId, LessonMaterialEntity source, UUID targetTopicId) {
        lessonMaterialRepository.saveAndFlush(
                new LessonMaterialEntity(
                        UUID.randomUUID(),
                        targetTopicId,
                        teacherId,
                        source.getMaterialType(),
                        source.getTitle(),
                        source.getContent(),
                        source.getFileAssetId(),
                        source.getExternalUrl(),
                        source.getPosition()));
    }

    private String copyTitle(String sourceTitle) {
        int maxBaseLength = MAX_TITLE_LENGTH - COPY_SUFFIX.length();
        String base =
                sourceTitle.length() <= maxBaseLength
                        ? sourceTitle
                        : sourceTitle.substring(0, maxBaseLength).stripTrailing();
        return base + COPY_SUFFIX;
    }

    private UUID teacherId(AuthenticatedUser principal) {
        return ownershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
    }
}
