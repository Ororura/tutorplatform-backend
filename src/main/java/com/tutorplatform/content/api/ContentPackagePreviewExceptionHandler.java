package com.tutorplatform.content.api;

import com.tutorplatform.content.api.response.ContentPackagePreviewFailureResponse;
import com.tutorplatform.content.api.response.ContentPackagePreviewResponse.ContentPackagePreviewError;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import com.tutorplatform.content.application.importpackage.ContentPackageValidationException;
import com.tutorplatform.shared.api.ApiError;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = TeacherContentPackagePreviewController.class)
@Order(-1)
public class ContentPackagePreviewExceptionHandler {
    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ContentPackagePreviewFailureResponse> handleMissingFile(
            MissingServletRequestPartException exception) {
        return ResponseEntity.badRequest()
                .body(
                        new ContentPackagePreviewFailureResponse(
                                List.of(
                                        new ContentPackagePreviewError(
                                                "REQUIRED_FIELD",
                                                exception.getRequestPartName(),
                                                "File is required"))));
    }

    @ExceptionHandler(ContentPackageParseException.class)
    ResponseEntity<?> handleParse(ContentPackageParseException exception) {
        if (exception.code() == ContentPackageParseException.Code.FILE_TOO_LARGE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(
                            ApiError.of(
                                    "FILE_TOO_LARGE", exception.getMessage(), MDC.get("traceId")));
        }
        return ResponseEntity.badRequest()
                .body(
                        new ContentPackagePreviewFailureResponse(
                                List.of(
                                        new ContentPackagePreviewError(
                                                exception.code().name(),
                                                exception.location(),
                                                exception.getMessage()))));
    }

    @ExceptionHandler(ContentPackageValidationException.class)
    ResponseEntity<ContentPackagePreviewFailureResponse> handleValidation(
            ContentPackageValidationException exception) {
        return ResponseEntity.badRequest()
                .body(
                        new ContentPackagePreviewFailureResponse(
                                exception.result().errors().stream()
                                        .map(
                                                error ->
                                                        new ContentPackagePreviewError(
                                                                error.code().name(),
                                                                error.path(),
                                                                error.message()))
                                        .toList()));
    }
}
