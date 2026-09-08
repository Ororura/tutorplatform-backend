package com.tutorplatform.shared.api;

import com.tutorplatform.auth.application.EmailAlreadyRegisteredException;
import com.tutorplatform.auth.application.InvalidCredentialsException;
import com.tutorplatform.content.application.FileTooLargeException;
import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.application.exception.LessonMaterialNotFoundException;
import com.tutorplatform.content.application.exception.LessonMaterialPositionConflictException;
import com.tutorplatform.content.application.exception.LessonMaterialVersionConflictException;
import com.tutorplatform.content.application.exception.TopicNotFoundException;
import com.tutorplatform.file.application.FileStorageException;
import com.tutorplatform.session.application.exception.InvalidLessonSessionTopicsException;
import com.tutorplatform.session.application.exception.InvalidSessionListParameterException;
import com.tutorplatform.session.application.exception.LessonSessionNotFoundException;
import com.tutorplatform.session.application.exception.StudentProgramNotFoundException;
import com.tutorplatform.session.application.exception.TopicOutsideStudentProgramException;
import com.tutorplatform.student.application.exception.InvalidStudentListParameterException;
import com.tutorplatform.student.application.exception.PublicStudentInviteAlreadyAcceptedException;
import com.tutorplatform.student.application.exception.StudentAlreadyRegisteredException;
import com.tutorplatform.student.application.exception.StudentInviteAlreadyAcceptedException;
import com.tutorplatform.student.application.exception.StudentInviteEmailConflictException;
import com.tutorplatform.student.application.exception.StudentInviteExpiredException;
import com.tutorplatform.student.application.exception.StudentInviteNotFoundException;
import com.tutorplatform.student.application.exception.StudentInviteRevokedException;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.task.application.exception.InvalidTaskException;
import com.tutorplatform.task.application.exception.InvalidTaskListParameterException;
import com.tutorplatform.task.application.exception.TaskAlreadyAttachedException;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.application.exception.TaskSubjectMismatchException;
import com.tutorplatform.task.application.exception.TaskSubjectNotFoundException;
import com.tutorplatform.task.application.exception.TaskTopicNotFoundException;
import com.tutorplatform.task.application.exception.TaskTopicPositionConflictException;
import com.tutorplatform.task.application.exception.TaskVersionConflictException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({FileTooLargeException.class,
            MaxUploadSizeExceededException.class})
    ResponseEntity<ApiError> handleFileTooLarge(Exception exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ApiError.of("FILE_TOO_LARGE", "File exceeds upload limit", MDC.get("traceId")));
    }

    @ExceptionHandler({MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> handleMissingUploadParameter(Exception exception) {
        return ResponseEntity.badRequest().body(
            ApiError.of("VALIDATION_ERROR", "Required request part or parameter is missing", MDC.get("traceId")));
    }

    @ExceptionHandler(FileStorageException.class)
    ResponseEntity<ApiError> handleStorageFailure(Exception exception) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("File storage operation failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError.of("FILE_STORAGE_ERROR", "File storage operation failed", MDC.get("traceId")));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of("EMAIL_ALREADY_REGISTERED", "Email is already registered", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of("AUTH_INVALID_CREDENTIALS", "Invalid email or password", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                details
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(
                ApiError.of("VALIDATION_ERROR", "Request body has an invalid value", MDC.get("traceId"))
        );
    }

    @ExceptionHandler({InvalidTaskException.class, InvalidTaskListParameterException.class})
    ResponseEntity<ApiError> handleInvalidTask(RuntimeException exception) {
        String field = exception instanceof InvalidTaskException invalid
                ? invalid.getField()
                : ((InvalidTaskListParameterException) exception).getField();
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(field, exception.getMessage()))
        ));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TASK_NOT_FOUND", "Task not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskSubjectNotFoundException.class)
    ResponseEntity<ApiError> handleTaskSubjectNotFound(TaskSubjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("SUBJECT_NOT_FOUND", "Subject not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskTopicNotFoundException.class)
    ResponseEntity<ApiError> handleTaskTopicNotFound(TaskTopicNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TOPIC_NOT_FOUND", "Topic not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskSubjectMismatchException.class)
    ResponseEntity<ApiError> handleTaskSubjectMismatch(TaskSubjectMismatchException exception) {
        return ResponseEntity.badRequest().body(
                ApiError.of(
                        "TASK_SUBJECT_MISMATCH",
                        "Task and topic must belong to the same subject",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(TaskTopicPositionConflictException.class)
    ResponseEntity<ApiError> handleTaskTopicPositionConflict(TaskTopicPositionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "TASK_TOPIC_POSITION_CONFLICT",
                        "Task position is already used by this topic",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(TaskAlreadyAttachedException.class)
    ResponseEntity<ApiError> handleTaskAlreadyAttached(TaskAlreadyAttachedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of("TASK_ALREADY_ATTACHED", "Task is already attached to this topic", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TaskVersionConflictException.class)
    ResponseEntity<ApiError> handleTaskVersionConflict(TaskVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "TASK_VERSION_CONFLICT",
                        "Task was modified by another request",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorDetail> details = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                details
        ));
    }

    @ExceptionHandler(InvalidStudentListParameterException.class)
    ResponseEntity<ApiError> handleInvalidStudentListParameter(InvalidStudentListParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(InvalidSessionListParameterException.class)
    ResponseEntity<ApiError> handleInvalidSessionListParameter(InvalidSessionListParameterException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(InvalidLessonMaterialException.class)
    ResponseEntity<ApiError> handleInvalidLessonMaterial(InvalidLessonMaterialException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getField(), exception.getMessage()))
        ));
    }

    @ExceptionHandler(TopicNotFoundException.class)
    ResponseEntity<ApiError> handleTopicNotFound(TopicNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("TOPIC_NOT_FOUND", "Topic not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonMaterialNotFoundException.class)
    ResponseEntity<ApiError> handleLessonMaterialNotFound(LessonMaterialNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(
                        "LESSON_MATERIAL_NOT_FOUND",
                        "Lesson material not found",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(LessonMaterialPositionConflictException.class)
    ResponseEntity<ApiError> handleLessonMaterialPositionConflict(
            LessonMaterialPositionConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "LESSON_MATERIAL_POSITION_CONFLICT",
                        "Material position is already used by this topic",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(LessonMaterialVersionConflictException.class)
    ResponseEntity<ApiError> handleLessonMaterialVersionConflict(
            LessonMaterialVersionConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "LESSON_MATERIAL_VERSION_CONFLICT",
                        "Lesson material was modified by another request",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(InvalidLessonSessionTopicsException.class)
    ResponseEntity<ApiError> handleInvalidLessonSessionTopics(InvalidLessonSessionTopicsException exception) {
        return ResponseEntity.badRequest().body(
                ApiError.of("LESSON_SESSION_TOPICS_INVALID", exception.getMessage(), MDC.get("traceId"))
        );
    }

    @ExceptionHandler(TopicOutsideStudentProgramException.class)
    ResponseEntity<ApiError> handleTopicOutsideStudentProgram(TopicOutsideStudentProgramException exception) {
        return ResponseEntity.badRequest().body(
                ApiError.of(
                        "LESSON_SESSION_TOPIC_INVALID",
                        "Topic does not belong to the student program",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(StudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleStudentProgramNotFound(StudentProgramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LessonSessionNotFoundException.class)
    ResponseEntity<ApiError> handleLessonSessionNotFound(LessonSessionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("LESSON_SESSION_NOT_FOUND", "Lesson session not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleLessonSessionVersionConflict(
            ObjectOptimisticLockingFailureException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "LESSON_SESSION_VERSION_CONFLICT",
                        "Lesson session was modified by another request",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                Instant.now(),
                MDC.get("traceId"),
                List.of(new ApiErrorDetail(exception.getName(), "has an invalid value"))
        ));
    }

    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> handleStudentNotFound(StudentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("STUDENT_NOT_FOUND", "Student not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleStudentAlreadyRegistered(StudentAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of("STUDENT_ALREADY_REGISTERED", "Student is already registered", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentInviteNotFoundException.class)
    ResponseEntity<ApiError> handleStudentInviteNotFound(StudentInviteNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of("STUDENT_INVITE_NOT_FOUND", "Student invitation not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentInviteAlreadyAcceptedException.class)
    ResponseEntity<ApiError> handleStudentInviteAlreadyAccepted(StudentInviteAlreadyAcceptedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "STUDENT_INVITE_ALREADY_ACCEPTED",
                        "Student invitation has already been accepted",
                        MDC.get("traceId")
                )
        );
    }

    @ExceptionHandler(PublicStudentInviteAlreadyAcceptedException.class)
    ResponseEntity<ApiError> handlePublicStudentInviteAlreadyAccepted(
            PublicStudentInviteAlreadyAcceptedException exception
    ) {
        return gone("STUDENT_INVITE_ALREADY_ACCEPTED", "Student invitation has already been accepted");
    }

    @ExceptionHandler(StudentInviteExpiredException.class)
    ResponseEntity<ApiError> handleStudentInviteExpired(StudentInviteExpiredException exception) {
        return gone("STUDENT_INVITE_EXPIRED", "Student invitation has expired");
    }

    @ExceptionHandler(StudentInviteRevokedException.class)
    ResponseEntity<ApiError> handleStudentInviteRevoked(StudentInviteRevokedException exception) {
        return gone("STUDENT_INVITE_REVOKED", "Student invitation has been revoked");
    }

    @ExceptionHandler(StudentInviteEmailConflictException.class)
    ResponseEntity<ApiError> handleStudentInviteEmailConflict(StudentInviteEmailConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(
                        "STUDENT_INVITE_EMAIL_CONFLICT",
                        "Student invitation email is already registered",
                        MDC.get("traceId")
                )
        );
    }

    private ResponseEntity<ApiError> gone(String code, String message) {
        return ResponseEntity.status(HttpStatus.GONE).body(ApiError.of(code, message, MDC.get("traceId")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.of("INTERNAL_ERROR", "Unexpected server failure", MDC.get("traceId"))
        );
    }
}
