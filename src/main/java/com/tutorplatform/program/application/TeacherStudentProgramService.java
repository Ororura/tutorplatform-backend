package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.api.AssignStudentProgramRequest;
import com.tutorplatform.program.api.ProgramModuleResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.api.ProgramTopicResponse;
import com.tutorplatform.program.api.StudentProgramDetailsResponse;
import com.tutorplatform.program.api.StudentProgramSummaryResponse;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherStudentProgramService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final TeacherStudentProgramQuery programQuery;
    private final TeacherLearningProgramQuery learningProgramQuery;
    private final LearningProgramRepository learningProgramRepository;
    private final StudentProgramRepository studentProgramRepository;
    private final StudentTopicProgressRepository progressRepository;

    public TeacherStudentProgramService(
        StudentOwnershipQuery studentOwnershipQuery,
        TeacherStudentProgramQuery programQuery,
        TeacherLearningProgramQuery learningProgramQuery,
        LearningProgramRepository learningProgramRepository,
        StudentProgramRepository studentProgramRepository,
        StudentTopicProgressRepository progressRepository
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.learningProgramQuery = learningProgramQuery;
        this.learningProgramRepository = learningProgramRepository;
        this.studentProgramRepository = studentProgramRepository;
        this.progressRepository = progressRepository;
    }

    public List<StudentProgramSummaryResponse> listPrograms(
        AuthenticatedUser principal,
        UUID studentId
    ) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        return programQuery.findPrograms(teacherId, studentId).stream()
            .map(TeacherStudentProgramService::toSummaryResponse)
            .toList();
    }

    public StudentProgramDetailsResponse getProgram(
        AuthenticatedUser principal,
        UUID studentId,
        UUID studentProgramId
    ) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        return programQuery.findProgram(teacherId, studentId, studentProgramId)
            .map(TeacherStudentProgramService::toDetailsResponse)
            .orElseThrow(StudentProgramNotFoundException::new);
    }

    public List<StudentProgramSummaryResponse> listProgramsForStudent(
        AuthenticatedUser principal
    ) {
        UUID studentId = currentStudentId(principal);
        return programQuery.findProgramsByStudentId(studentId).stream()
            .map(TeacherStudentProgramService::toSummaryResponse)
            .toList();
    }

    public StudentProgramDetailsResponse getProgramForStudent(
        AuthenticatedUser principal,
        UUID studentProgramId
    ) {
        UUID studentId = currentStudentId(principal);
        return programQuery.findProgramByStudentId(studentId, studentProgramId)
            .map(TeacherStudentProgramService::toDetailsResponse)
            .orElseThrow(StudentProgramNotFoundException::new);
    }

    @Transactional
    public StudentProgramSummaryResponse assign(
        AuthenticatedUser principal,
        UUID studentId,
        AssignStudentProgramRequest request
    ) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        LearningProgramEntity learningProgram = learningProgramRepository.findByIdForUpdate(request.learningProgramId())
            .filter(program -> program.getTeacherId().equals(teacherId))
            .orElseThrow(LearningProgramNotFoundException::new);
        if (learningProgram.getStatus() != LearningProgramStatus.ACTIVE) {
            throw new InvalidLearningProgramStatusException("Only an active learning program can be assigned");
        }
        if (studentProgramRepository.existsActiveOrPaused(studentId, learningProgram.getId())) {
            throw new StudentProgramAlreadyAssignedException();
        }

        int reportInterval = request.reportIntervalMinutes() == null
            ? StudentProgramEntity.DEFAULT_REPORT_INTERVAL_MINUTES
            : request.reportIntervalMinutes();
        StudentProgramEntity studentProgram;
        try {
            studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
                UUID.randomUUID(), studentId, learningProgram.getId(), teacherId,
                StudentProgramStatus.ACTIVE, reportInterval, Instant.now(), null
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new StudentProgramAlreadyAssignedException(exception);
        }
        for (UUID topicId : learningProgramQuery.findTopicIds(learningProgram.getId())) {
            progressRepository.saveAndFlush(new StudentTopicProgressEntity(
                studentProgram.id(), topicId, StudentTopicProgressStatus.LOCKED, null, null
            ));
        }

        return programQuery.findProgram(teacherId, studentId, studentProgram.id())
            .map(TeacherStudentProgramService::toSummaryResponse)
            .orElseThrow(StudentProgramNotFoundException::new);
    }

    private UUID requireOwnedStudent(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
        return teacherId;
    }

    private UUID currentStudentId(AuthenticatedUser principal) {
        return studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
    }

    private static StudentProgramSummaryResponse toSummaryResponse(
        TeacherStudentProgramQuery.StudentProgramSummary program
    ) {
        return new StudentProgramSummaryResponse(
            program.id(),
            program.learningProgramId(),
            program.title(),
            program.description(),
            program.status(),
            program.reportIntervalMinutes(),
            program.startedAt(),
            program.completedAt(),
            toSubjectResponse(program.subject())
        );
    }

    private static StudentProgramSummaryResponse toSummaryResponse(
        TeacherStudentProgramQuery.StudentProgramDetails program
    ) {
        return new StudentProgramSummaryResponse(
            program.id(), program.learningProgramId(), program.title(), program.description(), program.status(),
            program.reportIntervalMinutes(), program.startedAt(), program.completedAt(),
            toSubjectResponse(program.subject())
        );
    }

    private static StudentProgramDetailsResponse toDetailsResponse(
        TeacherStudentProgramQuery.StudentProgramDetails program
    ) {
        return new StudentProgramDetailsResponse(
            program.id(),
            program.learningProgramId(),
            program.title(),
            program.description(),
            program.status(),
            program.reportIntervalMinutes(),
            program.startedAt(),
            program.completedAt(),
            toSubjectResponse(program.subject()),
            program.modules().stream().map(module -> new ProgramModuleResponse(
                module.id(),
                module.title(),
                module.description(),
                module.position(),
                module.topics().stream().map(topic -> new ProgramTopicResponse(
                    topic.id(),
                    topic.title(),
                    topic.description(),
                    topic.position(),
                    topic.topicStatus(),
                    topic.progressStatus()
                )).toList()
            )).toList()
        );
    }

    private static ProgramSubjectResponse toSubjectResponse(TeacherStudentProgramQuery.ProgramSubject subject) {
        return new ProgramSubjectResponse(subject.id(), subject.code(), subject.name());
    }
}
