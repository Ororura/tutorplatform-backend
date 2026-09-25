package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.response.ContentPackagePreviewResponse;
import com.tutorplatform.content.application.importpackage.ContentPackagePreviewService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/teacher/programs/{programId}/imports")
public class TeacherContentPackagePreviewController implements TeacherContentPackagePreviewApi {
    private final ContentPackagePreviewService previewService;

    public TeacherContentPackagePreviewController(ContentPackagePreviewService previewService) {
        this.previewService = previewService;
    }

    @Override
    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    public ContentPackagePreviewResponse preview(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID programId,
            @RequestPart("file") MultipartFile file) {
        return ContentPackagePreviewResponse.from(
                previewService.preview(principal, programId, file));
    }
}
