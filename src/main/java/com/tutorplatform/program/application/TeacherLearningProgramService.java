package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.api.CreateLearningProgramRequest;
import com.tutorplatform.program.api.LearningProgramSummaryResponse;
import com.tutorplatform.program.api.LearningProgramDetailsResponse;
import com.tutorplatform.program.api.LearningProgramModuleDetailsResponse;
import com.tutorplatform.program.api.LearningProgramTopicDetailsResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherLearningProgramService {
    private final StudentOwnershipQuery ownershipQuery;
    private final SubjectRepository subjectRepository;
    private final LearningProgramRepository learningProgramRepository;
    private final TeacherLearningProgramQuery programQuery;

    public TeacherLearningProgramService(
        StudentOwnershipQuery ownershipQuery,
        SubjectRepository subjectRepository,
        LearningProgramRepository learningProgramRepository,
        TeacherLearningProgramQuery programQuery
    ) {
        this.ownershipQuery = ownershipQuery;
        this.subjectRepository = subjectRepository;
        this.learningProgramRepository = learningProgramRepository;
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
