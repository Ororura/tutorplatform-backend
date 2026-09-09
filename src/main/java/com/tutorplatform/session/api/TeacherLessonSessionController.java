package com.tutorplatform.session.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.session.api.request.CreateLessonSessionRequest;
import com.tutorplatform.session.api.request.LessonSessionTopicRequest;
import com.tutorplatform.session.api.request.UpdateLessonSessionRequest;
import com.tutorplatform.session.api.response.LessonSessionDetailsResponse;
import com.tutorplatform.session.api.response.LessonSessionPageResponse;
import com.tutorplatform.session.application.CreateLessonSessionCommand;
import com.tutorplatform.session.application.LessonSessionService;
import com.tutorplatform.session.application.LessonSessionTopicInput;
import com.tutorplatform.session.application.UpdateLessonSessionCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/sessions")
public class TeacherLessonSessionController implements TeacherLessonSessionApi {

    private final LessonSessionService lessonSessionService;

    public TeacherLessonSessionController(LessonSessionService lessonSessionService) {
        this.lessonSessionService = lessonSessionService;
    }

    @Override
    @PostMapping
    public ResponseEntity<LessonSessionDetailsResponse> createLessonSession(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @Valid @RequestBody CreateLessonSessionRequest request
    ) {
        var created = lessonSessionService.createLessonSession(
            principal,
            new CreateLessonSessionCommand(
                studentId,
                request.studentProgramId(),
                request.startedAt(),
                request.durationMinutes(),
                request.attendanceStatus(),
                request.summary(),
                request.privateNotes(),
                toTopicInputs(request.topics())
            )
        );
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/students/" + studentId + "/sessions/" + created.id()
        )).body(LessonSessionDetailsResponse.from(created));
    }

    @Override
    @GetMapping
    public LessonSessionPageResponse listLessonSessions(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "startedAt,desc") String sort
    ) {
        return LessonSessionPageResponse.from(lessonSessionService.listLessonSessions(
            principal, studentId, page, size, sort
        ));
    }

    @Override
    @GetMapping("/{sessionId}")
    public LessonSessionDetailsResponse getLessonSession(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID sessionId
    ) {
        return LessonSessionDetailsResponse.from(lessonSessionService.getLessonSession(
            principal, studentId, sessionId
        ));
    }

    @Override
    @PatchMapping("/{sessionId}")
    public LessonSessionDetailsResponse updateLessonSession(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID studentId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody UpdateLessonSessionRequest request
    ) {
        return LessonSessionDetailsResponse.from(lessonSessionService.updateLessonSession(
            principal,
            studentId,
            sessionId,
            new UpdateLessonSessionCommand(
                request.startedAt(),
                request.durationMinutes(),
                request.attendanceStatus(),
                request.summary(),
                request.privateNotes(),
                request.version(),
                toTopicInputs(request.topics())
            )
        ));
    }

    private List<LessonSessionTopicInput> toTopicInputs(List<LessonSessionTopicRequest> topics) {
        return topics.stream()
            .map(topic -> new LessonSessionTopicInput(topic.topicId(), topic.primary()))
            .toList();
    }
}
