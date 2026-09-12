package com.tutorplatform.progress.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.progress.application.exception.ProgressStudentProgramNotFoundException;
import com.tutorplatform.progress.domain.CurrentProgress;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProgressAccessService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final GetCurrentProgressService getCurrentProgressService;

    public ProgressAccessService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        GetCurrentProgressService getCurrentProgressService
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.getCurrentProgressService = getCurrentProgressService;
    }

    public CurrentProgress getForTeacher(
        AuthenticatedUser principal,
        UUID studentId,
        UUID studentProgramId
    ) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
        requireStudentProgramOwner(studentId, studentProgramId);
        return getCurrentProgressService.getCurrentProgress(studentProgramId);
    }

    public CurrentProgress getForStudent(AuthenticatedUser principal, UUID studentProgramId) {
        UUID studentId = studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
        requireStudentProgramOwner(studentId, studentProgramId);
        return getCurrentProgressService.getCurrentProgress(studentProgramId);
    }

    private void requireStudentProgramOwner(UUID studentId, UUID studentProgramId) {
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(studentProgramId)
            .orElseThrow(ProgressStudentProgramNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId)) {
            throw new ProgressStudentProgramNotFoundException();
        }
    }
}
