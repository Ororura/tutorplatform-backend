package com.tutorplatform.assessment.api;

import com.tutorplatform.assessment.application.SaveTeacherAssessmentCommand;
import com.tutorplatform.assessment.application.TeacherAssessmentService;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/sessions/{sessionId}/assessment")
public class TeacherAssessmentController implements TeacherAssessmentApi {

    private final TeacherAssessmentService assessmentService;

    public TeacherAssessmentController(TeacherAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @Override
    @GetMapping
    public TeacherAssessmentResponse getTeacherAssessment(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID sessionId
    ) {
        return TeacherAssessmentResponse.from(assessmentService.getTeacherAssessment(
            principal, studentId, sessionId
        ));
    }

    @Override
    @PutMapping
    public ResponseEntity<TeacherAssessmentResponse> saveTeacherAssessment(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody SaveTeacherAssessmentRequest request
    ) {
        var saved = assessmentService.saveTeacherAssessment(
            principal,
            studentId,
            sessionId,
            new SaveTeacherAssessmentCommand(
                request.understandingScore(), request.independenceScore(),
                request.practiceScore(), request.homeworkScore(), request.publicComment()
            )
        );
        var response = TeacherAssessmentResponse.from(saved.assessment());
        if (saved.created()) {
            return ResponseEntity.created(URI.create(
                "/api/v1/teacher/students/" + studentId + "/sessions/" + sessionId + "/assessment"
            )).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
