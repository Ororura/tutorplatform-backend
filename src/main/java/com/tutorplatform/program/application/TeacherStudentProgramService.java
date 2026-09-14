package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.api.ProgramModuleResponse;
import com.tutorplatform.program.api.ProgramSubjectResponse;
import com.tutorplatform.program.api.ProgramTopicResponse;
import com.tutorplatform.program.api.StudentProgramDetailsResponse;
import com.tutorplatform.program.api.StudentProgramSummaryResponse;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherStudentProgramService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final TeacherStudentProgramQuery programQuery;

    public TeacherStudentProgramService(
        StudentOwnershipQuery studentOwnershipQuery,
        TeacherStudentProgramQuery programQuery
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
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

    private UUID requireOwnedStudent(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
        return teacherId;
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
