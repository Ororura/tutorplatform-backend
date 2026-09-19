package com.tutorplatform.program.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/programs")
public class TeacherLearningProgramController implements TeacherLearningProgramApi {
    private final TeacherLearningProgramService service;

    public TeacherLearningProgramController(TeacherLearningProgramService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public List<LearningProgramSummaryResponse> listPrograms(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestParam(required = false) LearningProgramStatus status
    ) {
        return service.list(principal, status);
    }

    @Override
    @GetMapping("/{programId}")
    public LearningProgramDetailsResponse getProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId
    ) {
        return service.get(principal, programId);
    }

    @Override
    @PostMapping
    public ResponseEntity<LearningProgramSummaryResponse> createProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateLearningProgramRequest request
    ) {
        LearningProgramSummaryResponse response = service.create(principal, request);
        return ResponseEntity.created(URI.create("/api/v1/teacher/programs/" + response.id())).body(response);
    }

    @Override
    @PostMapping("/{programId}/modules")
    public ResponseEntity<LearningProgramModuleResponse> createModule(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId,
        @Valid @RequestBody CreateLearningProgramModuleRequest request
    ) {
        LearningProgramModuleResponse response = service.createModule(principal, programId, request);
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/programs/" + programId + "/modules/" + response.id()
        )).body(response);
    }

    @Override
    @PostMapping("/{programId}/modules/{moduleId}/topics")
    public ResponseEntity<LearningProgramTopicDetailsResponse> createTopic(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId,
        @PathVariable UUID moduleId,
        @Valid @RequestBody CreateLearningProgramTopicRequest request
    ) {
        LearningProgramTopicDetailsResponse response = service.createTopic(principal, programId, moduleId, request);
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/programs/" + programId + "/modules/" + moduleId + "/topics/" + response.id()
        )).body(response);
    }

    @Override
    @PatchMapping("/{programId}/modules/{moduleId}")
    public LearningProgramModuleResponse updateModule(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId,
        @PathVariable UUID moduleId,
        @Valid @RequestBody UpdateLearningProgramModuleRequest request
    ) {
        return service.updateModule(principal, programId, moduleId, request);
    }

    @Override
    @DeleteMapping("/{programId}/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId,
        @PathVariable UUID moduleId
    ) {
        service.deleteModule(principal, programId, moduleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{programId}")
    public LearningProgramDetailsResponse updateProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId,
        @Valid @RequestBody UpdateLearningProgramRequest request
    ) {
        return service.update(principal, programId, request);
    }

    @Override
    @PostMapping("/{programId}/activate")
    public LearningProgramSummaryResponse activateProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId
    ) {
        return service.activate(principal, programId);
    }

    @Override
    @PostMapping("/{programId}/archive")
    public LearningProgramSummaryResponse archiveProgram(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID programId
    ) {
        return service.archive(principal, programId);
    }
}
