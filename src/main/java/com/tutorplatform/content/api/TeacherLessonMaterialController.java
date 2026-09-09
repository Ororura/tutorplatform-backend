package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.request.CreateLessonMaterialRequest;
import com.tutorplatform.content.api.request.UpdateLessonMaterialRequest;
import com.tutorplatform.content.api.response.LessonMaterialResponse;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.FileMaterialService;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.content.application.UpdateLessonMaterialCommand;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.file.application.FileStorageException;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/topics/{topicId}/materials")
public class TeacherLessonMaterialController implements TeacherLessonMaterialApi {

    private final LessonMaterialService lessonMaterialService;
    private final FileMaterialService fileMaterialService;

    public TeacherLessonMaterialController(LessonMaterialService lessonMaterialService,
                                           FileMaterialService fileMaterialService) {
        this.lessonMaterialService = lessonMaterialService;
        this.fileMaterialService = fileMaterialService;
    }

    @Override
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<LessonMaterialResponse> uploadLessonMaterial(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @RequestParam LessonMaterialType materialType,
        @RequestParam String title,
        @RequestParam int position,
        @RequestPart MultipartFile file
    ) {
        try (var input = file.getInputStream()) {
            var created = LessonMaterialResponse.from(fileMaterialService.upload(
                principal,
                topicId,
                materialType,
                title,
                position,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                input
            ));
            return ResponseEntity.created(URI.create("/api/v1/teacher/topics/" + topicId
                + "/materials/" + created.id())).body(created);
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }

    @Override
    @GetMapping("/{materialId}/download")
    public ResponseEntity<byte[]> downloadLessonMaterial(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID topicId,
        @PathVariable UUID materialId
    ) {
        var download = fileMaterialService.download(principal, topicId, materialId);
        // Filename is display metadata only. Strip path components for the browser's save dialog.
        String filename = download.filename().replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.mimeType()))
            .contentLength(download.content().length)
            .header("Content-Disposition", ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .header("Cache-Control", "no-store")
            .body(download.content());
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
