package com.tutorplatform.student.api.coderunner;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.coderunner.StudentRunCodeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/tasks")
public class StudentRunCodeController implements StudentRunCodeApi {

    private final StudentRunCodeService runCodeService;

    public StudentRunCodeController(StudentRunCodeService runCodeService) {
        this.runCodeService = runCodeService;
    }

    @Override
    @PostMapping("/{taskId}/run")
    public RunCodeResponse runCode(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID taskId,
        @Valid @RequestBody RunCodeRequest request
    ) {
        return RunCodeResponse.from(runCodeService.run(
            principal, taskId, request.homeworkItemId(), request.studentProgramId(),
            request.topicId(), request.sourceCode()
        ));
    }
}
