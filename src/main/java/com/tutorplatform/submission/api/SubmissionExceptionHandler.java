package com.tutorplatform.submission.api;

import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.shared.api.ApiErrorDetail;
import com.tutorplatform.submission.application.exception.*;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(0)
public class SubmissionExceptionHandler {

    @ExceptionHandler(InvalidSubmissionException.class)
    ResponseEntity<ApiError> handleInvalidSubmission(InvalidSubmissionException exception) {
        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                "VALIDATION_ERROR",
                                "Request validation failed",
                                Instant.now(),
                                MDC.get("traceId"),
                                List.of(
                                        new ApiErrorDetail(
                                                exception.getField(), exception.getMessage()))));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    ResponseEntity<ApiError> handleSubmissionNotFound(SubmissionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiError.of(
                                "SUBMISSION_NOT_FOUND",
                                "Submission not found",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(InvalidSubmissionReviewStatusException.class)
    ResponseEntity<ApiError> handleInvalidSubmissionReviewStatus(
            InvalidSubmissionReviewStatusException exception) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.of(
                                "INVALID_SUBMISSION_REVIEW_STATUS",
                                "Review status must be PASSED or FAILED",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(SubmissionNotReviewableException.class)
    ResponseEntity<ApiError> handleSubmissionNotReviewable(
            SubmissionNotReviewableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "SUBMISSION_NOT_REVIEWABLE",
                                "Submission is not reviewable",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(HomeworkItemNotFoundException.class)
    ResponseEntity<ApiError> handleHomeworkItemNotFound(HomeworkItemNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiError.of(
                                "HOMEWORK_ITEM_NOT_FOUND",
                                "Homework item not found",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(TextSubmissionRequiredException.class)
    ResponseEntity<ApiError> handleTextSubmissionRequired(
            TextSubmissionRequiredException exception) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.of(
                                "TEXT_SUBMISSION_REQUIRED",
                                "Task must be a text task",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(SubmissionContextInvalidException.class)
    ResponseEntity<ApiError> handleSubmissionContextInvalid(
            SubmissionContextInvalidException exception) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.of(
                                "SUBMISSION_CONTEXT_INVALID",
                                "Submission context is invalid",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(HomeworkNotSubmittableException.class)
    ResponseEntity<ApiError> handleHomeworkNotSubmittable(
            HomeworkNotSubmittableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.of(
                                "HOMEWORK_NOT_SUBMITTABLE",
                                "Homework does not accept submissions",
                                MDC.get("traceId")));
    }
}
