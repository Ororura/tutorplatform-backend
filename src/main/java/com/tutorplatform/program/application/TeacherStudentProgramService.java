package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.api.AssignStudentProgramRequest;
import com.tutorplatform.program.api.BulkUpdateStudentTopicAccessRequest;
import com.tutorplatform.program.api.ProgramModuleResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.api.ProgramTopicResponse;
import com.tutorplatform.program.api.StudentProgramDetailsResponse;
import com.tutorplatform.program.api.StudentProgramSummaryResponse;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TeacherStudentProgramService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final TeacherStudentProgramQuery programQuery;
    private final TeacherLearningProgramQuery learningProgramQuery;
    private final LearningProgramRepository learningProgramRepository;
    private final StudentProgramRepository studentProgramRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final TopicRepository topicRepository;

    public TeacherStudentProgramService(
            StudentOwnershipQuery studentOwnershipQuery,
            TeacherStudentProgramQuery programQuery,
            TeacherLearningProgramQuery learningProgramQuery,
            LearningProgramRepository learningProgramRepository,
            StudentProgramRepository studentProgramRepository,
            StudentTopicProgressRepository progressRepository,
            TopicRepository topicRepository) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.learningProgramQuery = learningProgramQuery;
        this.learningProgramRepository = learningProgramRepository;
        this.studentProgramRepository = studentProgramRepository;
        this.progressRepository = progressRepository;
        this.topicRepository = topicRepository;
    }

    public List<StudentProgramSummaryResponse> listPrograms(
            AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        return programQuery.findPrograms(teacherId, studentId).stream()
                .map(TeacherStudentProgramService::toSummaryResponse)
                .toList();
    }

    public StudentProgramDetailsResponse getProgram(
            AuthenticatedUser principal, UUID studentId, UUID studentProgramId) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        return programQuery
                .findProgram(teacherId, studentId, studentProgramId)
                .map(TeacherStudentProgramService::toDetailsResponse)
                .orElseThrow(StudentProgramNotFoundException::new);
    }

    public List<StudentProgramSummaryResponse> listProgramsForStudent(AuthenticatedUser principal) {
        UUID studentId = currentStudentId(principal);
        return programQuery.findProgramsByStudentId(studentId).stream()
                .map(TeacherStudentProgramService::toSummaryResponse)
                .toList();
    }

    public StudentProgramDetailsResponse getProgramForStudent(
            AuthenticatedUser principal, UUID studentProgramId) {
        UUID studentId = currentStudentId(principal);
        return programQuery
                .findProgramByStudentId(studentId, studentProgramId)
                .map(TeacherStudentProgramService::toDetailsResponse)
                .orElseThrow(StudentProgramNotFoundException::new);
    }

    @Transactional
    public StudentProgramSummaryResponse assign(
            AuthenticatedUser principal, UUID studentId, AssignStudentProgramRequest request) {
        UUID teacherId = requireOwnedStudent(principal, studentId);
        LearningProgramEntity learningProgram =
                learningProgramRepository
                        .findByIdForUpdate(request.learningProgramId())
                        .filter(program -> program.getTeacherId().equals(teacherId))
                        .orElseThrow(LearningProgramNotFoundException::new);
        if (learningProgram.getStatus() != LearningProgramStatus.ACTIVE) {
            throw new InvalidLearningProgramStatusException(
                    "Only an active learning program can be assigned");
        }
        if (studentProgramRepository.existsActiveOrPaused(studentId, learningProgram.getId())) {
            throw new StudentProgramAlreadyAssignedException();
        }

        int reportInterval =
                request.reportIntervalMinutes() == null
                        ? StudentProgramEntity.DEFAULT_REPORT_INTERVAL_MINUTES
                        : request.reportIntervalMinutes();
        StudentProgramEntity studentProgram;
        try {
            studentProgram =
                    studentProgramRepository.saveAndFlush(
                            new StudentProgramEntity(
                                    UUID.randomUUID(),
                                    studentId,
                                    learningProgram.getId(),
                                    teacherId,
                                    StudentProgramStatus.ACTIVE,
                                    reportInterval,
                                    Instant.now(),
                                    null));
        } catch (DataIntegrityViolationException exception) {
            throw new StudentProgramAlreadyAssignedException(exception);
        }
        for (UUID topicId : learningProgramQuery.findTopicIds(learningProgram.getId())) {
            progressRepository.saveAndFlush(
                    new StudentTopicProgressEntity(
                            studentProgram.id(),
                            topicId,
                            StudentTopicProgressStatus.LOCKED,
                            null,
                            null));
        }

        return programQuery
                .findProgram(teacherId, studentId, studentProgram.id())
                .map(TeacherStudentProgramService::toSummaryResponse)
                .orElseThrow(StudentProgramNotFoundException::new);
    }

    @Transactional
    public void bulkUpdateTopicAccess(
            AuthenticatedUser principal,
            UUID studentId,
            UUID studentProgramId,
            BulkUpdateStudentTopicAccessRequest request) {
        UUID teacherId = requireOwnedStudent(principal, studentId);

        StudentProgramEntity studentProgram =
                studentProgramRepository
                        .findByIdForUpdate(studentProgramId)
                        .filter(program -> program.studentId().equals(studentId))
                        .filter(program -> program.assignedByTeacherId().equals(teacherId))
                        .orElseThrow(StudentProgramNotFoundException::new);

        if (studentProgram.status() == StudentProgramStatus.COMPLETED
                || studentProgram.status() == StudentProgramStatus.ARCHIVED) {
            throw new StudentTopicAccessConflictException(
                    "Topic access cannot be changed for a completed or archived student program");
        }

        List<UUID> topicIds = request.topicIds();

        List<TopicEntity> topics =
                topicRepository.findByLearningProgramIdAndIdIn(
                        studentProgram.learningProgramId(), topicIds);

        if (topics.size() != topicIds.size()) {
            throw new LearningProgramTopicNotFoundException();
        }

        if (request.status().toProgressStatus() == StudentTopicProgressStatus.AVAILABLE
                && topics.stream().anyMatch(topic -> topic.status() != TopicStatus.ACTIVE)) {
            throw new StudentTopicAccessConflictException(
                    "Only active learning program topics can be made available");
        }

        List<StudentTopicProgressEntity> progressRows =
                progressRepository.findAllForUpdate(studentProgramId, topicIds);

        if (progressRows.size() != topicIds.size()) {
            throw new LearningProgramTopicNotFoundException();
        }

        for (StudentTopicProgressEntity progress : progressRows) {
            if (progress.status() == StudentTopicProgressStatus.IN_PROGRESS
                    || progress.status() == StudentTopicProgressStatus.COMPLETED) {
                throw new StudentTopicAccessConflictException(
                        "Access cannot be changed for a topic that has already been started or completed");
            }
        }

        StudentTopicProgressStatus targetStatus = request.status().toProgressStatus();

        for (StudentTopicProgressEntity progress : progressRows) {
            if (progress.status() == targetStatus) {
                continue;
            }

            progressRepository.saveAndFlush(
                    new StudentTopicProgressEntity(
                            progress.studentProgramId(),
                            progress.topicId(),
                            targetStatus,
                            null,
                            null,
                            progress.updatedAt()));
        }
    }

    private UUID requireOwnedStudent(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
        return teacherId;
    }

    private UUID currentStudentId(AuthenticatedUser principal) {
        return studentOwnershipQuery
                .findStudentIdByUserId(principal.id())
                .orElseThrow(StudentNotFoundException::new);
    }

    private static StudentProgramSummaryResponse toSummaryResponse(
            TeacherStudentProgramQuery.StudentProgramSummary program) {
        return new StudentProgramSummaryResponse(
                program.id(),
                program.learningProgramId(),
                program.title(),
                program.description(),
                program.status(),
                program.reportIntervalMinutes(),
                program.startedAt(),
                program.completedAt(),
                toSubjectResponse(program.subject()));
    }

    private static StudentProgramSummaryResponse toSummaryResponse(
            TeacherStudentProgramQuery.StudentProgramDetails program) {
        return new StudentProgramSummaryResponse(
                program.id(),
                program.learningProgramId(),
                program.title(),
                program.description(),
                program.status(),
                program.reportIntervalMinutes(),
                program.startedAt(),
                program.completedAt(),
                toSubjectResponse(program.subject()));
    }

    private static StudentProgramDetailsResponse toDetailsResponse(
            TeacherStudentProgramQuery.StudentProgramDetails program) {
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
                program.modules().stream()
                        .map(
                                module ->
                                        new ProgramModuleResponse(
                                                module.id(),
                                                module.title(),
                                                module.description(),
                                                module.position(),
                                                module.topics().stream()
                                                        .map(
                                                                topic ->
                                                                        new ProgramTopicResponse(
                                                                                topic.id(),
                                                                                topic.title(),
                                                                                topic.description(),
                                                                                topic.position(),
                                                                                topic.topicStatus(),
                                                                                topic
                                                                                        .progressStatus()))
                                                        .toList()))
                        .toList());
    }

    private static ProgramSubjectResponse toSubjectResponse(
            TeacherStudentProgramQuery.ProgramSubject subject) {
        return new ProgramSubjectResponse(subject.id(), subject.code(), subject.name());
    }
}
