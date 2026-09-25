package com.tutorplatform.content.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.api.response.ContentPackageImportResponse;
import com.tutorplatform.content.application.importpackage.ContentPackageImportService;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/teacher/programs/{programId}/imports")
public class TeacherContentPackageImportController implements TeacherContentPackageImportApi {
    private static final int MAX_BYTES = 1_048_576;
    private final ContentPackageImportService importService;

    public TeacherContentPackageImportController(ContentPackageImportService importService) {
        this.importService = importService;
    }

    @Override
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ContentPackageImportResponse> importPackage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID programId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("confirmationId") UUID confirmationId,
            @RequestParam("digest") String digest) {
        if (!digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("digest must be a lowercase SHA-256 hex value");
        }
        if (file.getSize() > MAX_BYTES) {
            throw fileTooLarge();
        }
        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException exception) {
            throw new ContentPackageParseException(
                    ContentPackageParseException.Code.FILE_READ_ERROR,
                    "file",
                    "Uploaded file could not be read");
        }
        if (bytes.length > MAX_BYTES) {
            throw fileTooLarge();
        }
        var result =
                importService.importPackage(principal, programId, confirmationId, digest, bytes);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(ContentPackageImportResponse.from(result));
    }

    private static ContentPackageParseException fileTooLarge() {
        return new ContentPackageParseException(
                ContentPackageParseException.Code.FILE_TOO_LARGE,
                "file",
                "YAML file exceeds 1 MiB");
    }
}
