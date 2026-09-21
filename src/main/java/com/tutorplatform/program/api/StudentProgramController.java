package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.StudentProgramTopicService;
import com.tutorplatform.program.application.TeacherStudentProgramService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/programs")
public class StudentProgramController implements StudentProgramApi {

    private final TeacherStudentProgramService programService;
    private final StudentProgramTopicService topicService;

    public StudentProgramController(
        TeacherStudentProgramService programService,
        StudentProgramTopicService topicService
    ) {
        this.programService = programService;
        this.topicService = topicService;
    }

    @Override
    @GetMapping
    public List<StudentProgramSummaryResponse> listPrograms(
        @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return programService.listProgramsForStudent(principal);
    }

    @Override
    @GetMapping("/{studentProgramId}")
    public StudentProgramDetailsResponse getProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentProgramId
    ) {
        return programService.getProgramForStudent(principal, studentProgramId);
    }

    @Override
    @GetMapping("/{studentProgramId}/topics/{topicId}")
    public StudentProgramTopicResponse getTopic(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentProgramId,
        @PathVariable UUID topicId
    ) {
        return topicService.getTopic(principal, studentProgramId, topicId);
    }
}
