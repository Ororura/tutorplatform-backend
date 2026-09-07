package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.request.CreateLessonMaterialRequest;
import com.tutorplatform.content.api.request.UpdateLessonMaterialRequest;
import com.tutorplatform.content.api.response.LessonMaterialResponse;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.application.UpdateLessonMaterialCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/topics/{topicId}/materials")
public class TeacherLessonMaterialController implements TeacherLessonMaterialApi {

    private final LessonMaterialService lessonMaterialService;

    public TeacherLessonMaterialController(LessonMaterialService lessonMaterialService) {
        this.lessonMaterialService = lessonMaterialService;
    }

    @Override
    @PostMapping
    public ResponseEntity<LessonMaterialResponse> createLessonMaterial(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @Valid @RequestBody CreateLessonMaterialRequest request
    ) {
        LessonMaterialResponse created = LessonMaterialResponse.from(
            lessonMaterialService.createLessonMaterial(
                principal,
                new CreateLessonMaterialCommand(
                    topicId,
                    request.materialType(),
                    request.title(),
                    request.content(),
                    null,
                    request.externalUrl(),
                    request.position()
                )
            )
        );
        return ResponseEntity.created(URI.create(
            "/api/v1/teacher/topics/" + topicId + "/materials/" + created.id()
        )).body(created);
    }

    @Override
    @GetMapping
    public List<LessonMaterialResponse> listLessonMaterials(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId
    ) {
        return lessonMaterialService.listLessonMaterials(principal, topicId).stream()
            .map(LessonMaterialResponse::from)
            .toList();
    }

    @Override
    @GetMapping("/{materialId}")
    public LessonMaterialResponse getLessonMaterial(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @PathVariable UUID materialId
    ) {
        return LessonMaterialResponse.from(
            lessonMaterialService.getLessonMaterial(principal, topicId, materialId)
        );
    }

    @Override
    @PatchMapping("/{materialId}")
    public LessonMaterialResponse updateLessonMaterial(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @PathVariable UUID materialId,
        @Valid @RequestBody UpdateLessonMaterialRequest request
    ) {
        return LessonMaterialResponse.from(lessonMaterialService.updateLessonMaterial(
            principal,
            topicId,
            materialId,
            new UpdateLessonMaterialCommand(
                request.materialType(),
                request.title(),
                request.content(),
                null,
                request.externalUrl(),
                request.position(),
                request.version()
            )
        ));
    }
}
