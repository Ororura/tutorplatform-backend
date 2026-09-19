package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.api.CreateLearningProgramRequest;
import com.tutorplatform.program.api.CreateLearningProgramModuleRequest;
import com.tutorplatform.program.api.CreateLearningProgramTopicRequest;
import com.tutorplatform.program.api.LearningProgramModuleResponse;
import com.tutorplatform.program.api.LearningProgramSummaryResponse;
import com.tutorplatform.program.api.LearningProgramDetailsResponse;
import com.tutorplatform.program.api.LearningProgramModuleDetailsResponse;
import com.tutorplatform.program.api.LearningProgramTopicDetailsResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.api.ReorderLearningProgramModulesRequest;
import com.tutorplatform.program.api.ReorderLearningProgramTopicsRequest;
import com.tutorplatform.program.api.UpdateLearningProgramRequest;
import com.tutorplatform.program.api.UpdateLearningProgramModuleRequest;
import com.tutorplatform.program.api.UpdateLearningProgramTopicRequest;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherLearningProgramService {
    private final StudentOwnershipQuery ownershipQuery;
    private final SubjectRepository subjectRepository;
    private final LearningProgramRepository learningProgramRepository;
    private final ModuleRepository moduleRepository;
    private final TopicRepository topicRepository;
    private final StudentProgramRepository studentProgramRepository;
    private final TeacherLearningProgramQuery programQuery;

    public TeacherLearningProgramService(
        StudentOwnershipQuery ownershipQuery,
        SubjectRepository subjectRepository,
        LearningProgramRepository learningProgramRepository,
        ModuleRepository moduleRepository,
        TopicRepository topicRepository,
        StudentProgramRepository studentProgramRepository,
        TeacherLearningProgramQuery programQuery
    ) {
        this.ownershipQuery = ownershipQuery;
        this.subjectRepository = subjectRepository;
        this.learningProgramRepository = learningProgramRepository;
        this.moduleRepository = moduleRepository;
        this.topicRepository = topicRepository;
        this.studentProgramRepository = studentProgramRepository;
        this.programQuery = programQuery;
    }

    public List<LearningProgramSummaryResponse> list(AuthenticatedUser principal, LearningProgramStatus status) {
        UUID teacherId = teacherId(principal);
        return programQuery.findPrograms(teacherId, status).stream().map(program ->
            new LearningProgramSummaryResponse(
                program.id(), new ProgramSubjectResponse(program.subjectId(), program.subjectCode(), program.subjectName()),
                program.title(), program.description(), program.status(), program.createdAt(), program.updatedAt()
            )).toList();
    }

    public LearningProgramDetailsResponse get(AuthenticatedUser principal, UUID programId) {
        UUID teacherId = teacherId(principal);
        TeacherLearningProgramQuery.LearningProgramDetails program = programQuery.findProgram(teacherId, programId)
            .orElseThrow(LearningProgramNotFoundException::new);
        boolean editable = program.status() != LearningProgramStatus.ARCHIVED && !program.hasAssignments();
        return new LearningProgramDetailsResponse(
            program.id(), new ProgramSubjectResponse(program.subjectId(), program.subjectCode(), program.subjectName()),
            program.title(), program.description(), program.status(), program.version(), program.createdAt(), program.updatedAt(),
            program.hasAssignments(), editable,
            program.modules().stream().map(module -> new LearningProgramModuleDetailsResponse(
                module.id(), module.title(), module.description(), module.position(),
                module.topics().stream().map(topic -> new LearningProgramTopicDetailsResponse(
                    topic.id(), topic.title(), topic.description(), topic.position(), topic.status(), topic.version()
                )).toList()
            )).toList()
        );
    }

    @Transactional
    public LearningProgramSummaryResponse create(AuthenticatedUser principal, CreateLearningProgramRequest request) {
        UUID teacherId = teacherId(principal);
        SubjectEntity subject = requireAccessibleActiveSubject(teacherId, request.subjectId());
        LearningProgramEntity saved = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacherId, subject.id(), request.title(), request.description(), LearningProgramStatus.DRAFT
        ));
        return response(saved, subject);
    }

    @Transactional
    public LearningProgramModuleResponse createModule(
        AuthenticatedUser principal,
        UUID programId,
        CreateLearningProgramModuleRequest request
    ) {
        UUID teacherId = teacherId(principal);
        requireEditableOwnedProgram(teacherId, programId);

        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), programId, request.title(), request.description(),
            moduleRepository.findMaxPositionByLearningProgramId(programId) + 1
        ));
        return new LearningProgramModuleResponse(module.id(), module.title(), module.description(), module.position());
    }

    @Transactional
    public LearningProgramTopicDetailsResponse createTopic(
        AuthenticatedUser principal,
        UUID programId,
        UUID moduleId,
        CreateLearningProgramTopicRequest request
    ) {
        ModuleEntity module = requireModuleInEditableOwnedProgram(teacherId(principal), programId, moduleId);
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), module.id(), request.title(), request.description(),
            topicRepository.findMaxPositionByModuleId(moduleId) + 1, TopicStatus.DRAFT
        ));
        return new LearningProgramTopicDetailsResponse(
            topic.id(), topic.title(), topic.description(), topic.position(), topic.status(), topic.version()
        );
    }

    @Transactional
    public LearningProgramModuleResponse updateModule(
        AuthenticatedUser principal,
        UUID programId,
        UUID moduleId,
        UpdateLearningProgramModuleRequest request
    ) {
        ModuleEntity module = requireModuleInEditableOwnedProgram(teacherId(principal), programId, moduleId);
        ModuleEntity saved = moduleRepository.saveAndFlush(new ModuleEntity(
            module.id(), module.learningProgramId(), request.title(), request.description(), module.position()
        ));
        return new LearningProgramModuleResponse(saved.id(), saved.title(), saved.description(), saved.position());
    }

    @Transactional
    public void reorderModules(
        AuthenticatedUser principal,
        UUID programId,
        ReorderLearningProgramModulesRequest request
    ) {
        requireEditableOwnedProgram(teacherId(principal), programId);

        List<ModuleEntity> modules = moduleRepository.findByLearningProgramId(programId);
        List<UUID> orderedIds = request.orderedIds();
        Set<UUID> existingIds = new HashSet<>(modules.stream().map(ModuleEntity::id).toList());
        Set<UUID> requestedIds = new HashSet<>(orderedIds);
        if (orderedIds.size() != modules.size()
            || requestedIds.size() != orderedIds.size()
            || !requestedIds.equals(existingIds)) {
            throw new InvalidLearningProgramModuleOrderException();
        }

        int maxPosition = modules.stream().mapToInt(ModuleEntity::position).max().orElse(-1);
        long highestTemporaryPosition = (long) maxPosition + orderedIds.size();
        if (highestTemporaryPosition > Integer.MAX_VALUE) {
            throw new InvalidLearningProgramModuleOrderException();
        }

        int temporaryBase = maxPosition + 1;
        for (int index = 0; index < orderedIds.size(); index++) {
            moduleRepository.updatePosition(programId, orderedIds.get(index), temporaryBase + index);
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            moduleRepository.updatePosition(programId, orderedIds.get(index), index);
        }
    }

    @Transactional
    public void reorderTopics(
        AuthenticatedUser principal,
        UUID programId,
        UUID moduleId,
        ReorderLearningProgramTopicsRequest request
    ) {
        requireModuleInEditableOwnedProgram(teacherId(principal), programId, moduleId);

        List<TopicEntity> topics = topicRepository.findByModuleId(moduleId);
        List<UUID> orderedIds = request.orderedIds();
        Set<UUID> existingIds = new HashSet<>(topics.stream().map(TopicEntity::id).toList());
        Set<UUID> requestedIds = new HashSet<>(orderedIds);
        if (orderedIds.size() != topics.size()
            || requestedIds.size() != orderedIds.size()
            || !requestedIds.equals(existingIds)) {
            throw new InvalidLearningProgramTopicOrderException();
        }

        int maxPosition = topics.stream().mapToInt(TopicEntity::position).max().orElse(-1);
        long highestTemporaryPosition = (long) maxPosition + orderedIds.size();
        if (highestTemporaryPosition > Integer.MAX_VALUE) {
            throw new InvalidLearningProgramTopicOrderException();
        }

        int temporaryBase = maxPosition + 1;
        for (int index = 0; index < orderedIds.size(); index++) {
            topicRepository.updatePosition(moduleId, orderedIds.get(index), temporaryBase + index);
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            topicRepository.updatePosition(moduleId, orderedIds.get(index), index);
        }
    }

    @Transactional
    public LearningProgramTopicDetailsResponse updateTopic(
        AuthenticatedUser principal,
        UUID programId,
        UUID moduleId,
        UUID topicId,
        UpdateLearningProgramTopicRequest request
    ) {
        ModuleEntity module = requireModuleInEditableOwnedProgram(teacherId(principal), programId, moduleId);
        TopicEntity topic = topicRepository.findById(topicId)
            .filter(candidate -> candidate.moduleId().equals(module.id()))
            .orElseThrow(LearningProgramTopicNotFoundException::new);
        if (!topic.version().equals(request.version())) {
            throw new LearningProgramTopicVersionConflictException();
        }

        TopicEntity updated = new TopicEntity(
            topic.id(), topic.moduleId(), request.title(), request.description(), topic.position(), request.status(),
            topic.version(), topic.createdAt(), topic.updatedAt()
        );
        try {
            updated = topicRepository.saveAndFlush(updated);
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw new LearningProgramTopicVersionConflictException(exception);
        }
        return new LearningProgramTopicDetailsResponse(
            updated.id(), updated.title(), updated.description(), updated.position(), updated.status(), updated.version()
        );
    }

    @Transactional
    public void deleteModule(AuthenticatedUser principal, UUID programId, UUID moduleId) {
        requireModuleInEditableOwnedProgram(teacherId(principal), programId, moduleId);
        if (topicRepository.existsByModuleId(moduleId)) {
            throw new LearningProgramModuleNotEmptyException();
        }
        moduleRepository.deleteById(moduleId);
    }

    @Transactional
    public LearningProgramDetailsResponse update(
        AuthenticatedUser principal,
        UUID programId,
        UpdateLearningProgramRequest request
    ) {
        UUID teacherId = teacherId(principal);
        LearningProgramEntity program = learningProgramRepository.findByIdForUpdate(programId)
            .filter(candidate -> candidate.getTeacherId().equals(teacherId))
            .orElseThrow(LearningProgramNotFoundException::new);
        if (program.getStatus() == LearningProgramStatus.ARCHIVED) {
            throw new InvalidLearningProgramStatusException("Archived learning program cannot be edited");
        }
        if (studentProgramRepository.existsByLearningProgramId(programId)) {
            throw new InvalidLearningProgramStatusException("Assigned learning program cannot be edited");
        }
        if (!program.getVersion().equals(request.version())) {
            throw new LearningProgramVersionConflictException();
        }

        program.update(request.title(), request.description());
        try {
            learningProgramRepository.saveAndFlush(program);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new LearningProgramVersionConflictException(exception);
        }
        return get(principal, programId);
    }

    @Transactional
    public LearningProgramSummaryResponse activate(AuthenticatedUser principal, UUID programId) {
        UUID teacherId = teacherId(principal);
        LearningProgramEntity program = requireOwnedProgram(teacherId, programId);
        try {
            program.activate();
        } catch (IllegalStateException exception) {
            throw new InvalidLearningProgramStatusException(exception.getMessage());
        }
        LearningProgramEntity saved = learningProgramRepository.saveAndFlush(program);
        SubjectEntity subject = subjectRepository.findById(saved.getSubjectId()).orElseThrow(SubjectNotFoundException::new);
        return response(saved, subject);
    }

    @Transactional
    public LearningProgramSummaryResponse archive(AuthenticatedUser principal, UUID programId) {
        UUID teacherId = teacherId(principal);
        LearningProgramEntity program = requireOwnedProgram(teacherId, programId);
        program.archive();
        LearningProgramEntity saved = learningProgramRepository.saveAndFlush(program);
        SubjectEntity subject = subjectRepository.findById(saved.getSubjectId()).orElseThrow(SubjectNotFoundException::new);
        return response(saved, subject);
    }

    private SubjectEntity requireAccessibleActiveSubject(UUID teacherId, UUID subjectId) {
        return subjectRepository.findById(subjectId)
            .filter(subject -> subject.status() == SubjectStatus.ACTIVE)
            .filter(subject -> subject.ownerTeacherId() == null || subject.ownerTeacherId().equals(teacherId))
            .orElseThrow(SubjectNotFoundException::new);
    }

    private LearningProgramEntity requireOwnedProgram(UUID teacherId, UUID programId) {
        return learningProgramRepository.findById(programId)
            .filter(program -> program.getTeacherId().equals(teacherId))
            .orElseThrow(LearningProgramNotFoundException::new);
    }

    private LearningProgramEntity requireEditableOwnedProgram(UUID teacherId, UUID programId) {
        LearningProgramEntity program = learningProgramRepository.findByIdForUpdate(programId)
            .filter(candidate -> candidate.getTeacherId().equals(teacherId))
            .orElseThrow(LearningProgramNotFoundException::new);
        if (program.getStatus() == LearningProgramStatus.ARCHIVED) {
            throw new InvalidLearningProgramStatusException("Archived learning program cannot be edited");
        }
        if (studentProgramRepository.existsByLearningProgramId(programId)) {
            throw new InvalidLearningProgramStatusException("Assigned learning program cannot be edited");
        }
        return program;
    }

    private ModuleEntity requireModuleInEditableOwnedProgram(UUID teacherId, UUID programId, UUID moduleId) {
        requireEditableOwnedProgram(teacherId, programId);
        return moduleRepository.findById(moduleId)
            .filter(module -> module.learningProgramId().equals(programId))
            .orElseThrow(LearningProgramModuleNotFoundException::new);
    }

    private UUID teacherId(AuthenticatedUser principal) {
        return ownershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
    }

    private LearningProgramSummaryResponse response(LearningProgramEntity program, SubjectEntity subject) {
        return new LearningProgramSummaryResponse(
            program.getId(), new ProgramSubjectResponse(subject.id(), subject.code(), subject.name()),
            program.getTitle(), program.getDescription(), program.getStatus(), program.getCreatedAt(), program.getUpdatedAt()
        );
    }
}
