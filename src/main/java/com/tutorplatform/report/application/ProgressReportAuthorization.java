package com.tutorplatform.report.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.report.application.exception.ProgressReportNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProgressReportAuthorization {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;

    ProgressReportAuthorization(
            StudentOwnershipQuery studentOwnershipQuery, ProgramQuery programQuery) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
    }

    AuthorizedProgram require(AuthenticatedUser principal, UUID studentId, UUID studentProgramId) {
        AuthorizedProgram authorized = require(principal, studentProgramId);
        if (!authorized.program().belongsToStudent(studentId)) {
            throw new ProgressReportNotFoundException();
        }
        return authorized;
    }

    AuthorizedProgram require(AuthenticatedUser principal, UUID studentProgramId) {
        UUID teacherId =
                studentOwnershipQuery
                        .findTeacherIdByUserId(principal.id())
                        .orElseThrow(ProgressReportNotFoundException::new);
        ProgramQuery.StudentProgramContext program =
                programQuery
                        .findStudentProgram(studentProgramId)
                        .orElseThrow(ProgressReportNotFoundException::new);
        if (!program.isAssignedBy(teacherId)
                || !studentOwnershipQuery.isActivePrimaryOwner(teacherId, program.studentId())) {
            throw new ProgressReportNotFoundException();
        }
        return new AuthorizedProgram(teacherId, program);
    }

    UUID currentTeacherId(AuthenticatedUser principal) {
        return studentOwnershipQuery
                .findTeacherIdByUserId(principal.id())
                .orElseThrow(ProgressReportNotFoundException::new);
    }

    record AuthorizedProgram(UUID teacherId, ProgramQuery.StudentProgramContext program) {}
}
