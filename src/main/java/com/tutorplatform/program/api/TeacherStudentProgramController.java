package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.TeacherStudentProgramService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/programs")
public class TeacherStudentProgramController implements TeacherStudentProgramApi {

    private final TeacherStudentProgramService programService;

    public TeacherStudentProgramController(TeacherStudentProgramService programService) {
        this.programService = programService;
    }

    @Override
    @GetMapping
    public List<StudentProgramSummaryResponse> listPrograms(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId
    ) {
        return programService.listPrograms(principal, studentId);
    }

    @Override
    @GetMapping("/{studentProgramId}")
    public StudentProgramDetailsResponse getProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID studentProgramId
    ) {
        return programService.getProgram(principal, studentId, studentProgramId);
    }
}
