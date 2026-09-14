package com.tutorplatform.program.api;

import com.tutorplatform.program.application.StudentProgramNotFoundException;
import com.tutorplatform.program.application.LearningProgramNotFoundException;
import com.tutorplatform.program.application.SubjectNotFoundException;
import com.tutorplatform.program.application.InvalidLearningProgramStatusException;
import com.tutorplatform.program.application.StudentProgramAlreadyAssignedException;
import com.tutorplatform.shared.api.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgramExceptionHandler {

    @ExceptionHandler(StudentProgramNotFoundException.class)
    ResponseEntity<ApiError> handleStudentProgramNotFound(StudentProgramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("STUDENT_PROGRAM_NOT_FOUND", "Student program not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(LearningProgramNotFoundException.class)
    ResponseEntity<ApiError> handleLearningProgramNotFound(LearningProgramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("LEARNING_PROGRAM_NOT_FOUND", "Learning program not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    ResponseEntity<ApiError> handleSubjectNotFound(SubjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError.of("SUBJECT_NOT_FOUND", "Subject not found", MDC.get("traceId"))
        );
    }

    @ExceptionHandler(InvalidLearningProgramStatusException.class)
    ResponseEntity<ApiError> handleInvalidStatus(InvalidLearningProgramStatusException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError.of("LEARNING_PROGRAM_STATUS_CONFLICT", exception.getMessage(), MDC.get("traceId"))
        );
    }

    @ExceptionHandler(StudentProgramAlreadyAssignedException.class)
    ResponseEntity<ApiError> handleAlreadyAssigned(StudentProgramAlreadyAssignedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError.of("STUDENT_PROGRAM_ALREADY_ASSIGNED", "Learning program is already assigned", MDC.get("traceId"))
        );
    }
}
