package com.tutorplatform.student.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import com.tutorplatform.student.api.requrest.CreateStudentInviteRequest;
import com.tutorplatform.student.api.requrest.CreateStudentRequest;
import com.tutorplatform.student.api.requrest.UpdateStudentRequest;
import com.tutorplatform.student.api.response.*;
import com.tutorplatform.student.application.StudentService;
import com.tutorplatform.student.application.StudentInviteService;
import com.tutorplatform.student.domain.StudentAccountStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students")
public class TeacherStudentController {

    private final StudentService studentService;
    private final StudentInviteService studentInviteService;

    public TeacherStudentController(StudentService studentService, StudentInviteService studentInviteService) {
        this.studentService = studentService;
        this.studentInviteService = studentInviteService;
    }

    @Operation(operationId = "listTeacherStudents", summary = "List students owned by the current teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student page"),
            @ApiResponse(responseCode = "400", description = "Invalid list parameters", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public StudentPageResponse listTeacherStudents(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Parameter(schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", minimum = "1", maximum = "100", defaultValue = "20"
            ))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(schema = @Schema(maxLength = 100))
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StudentAccountStatus accountStatus,
            @Parameter(schema = @Schema(defaultValue = "createdAt,desc", allowableValues = {
                    "createdAt,asc", "createdAt,desc",
                    "firstName,asc", "firstName,desc",
                    "lastName,asc", "lastName,desc"
            }))
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return studentService.listStudents(principal, page, size, query, accountStatus, sort);
    }

    @Operation(operationId = "createStudent", summary = "Create a student for the current teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentSummaryResponse> createStudent(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateStudentRequest request
    ) {
        StudentSummaryResponse response = studentService.createStudent(principal, request);
        return ResponseEntity.created(URI.create("/api/v1/teacher/students/" + response.id())).body(response);
    }

    @Operation(operationId = "getStudent", summary = "Get a student owned by the current teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student details"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found or not owned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/{studentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public StudentDetailsResponse getStudent(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId
    ) {
        return studentService.getStudent(principal, studentId);
    }

    @Operation(operationId = "updateStudent", summary = "Update a student owned by the current teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found or not owned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping(value = "/{studentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public UpdateStudentResponse updateStudent(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateStudentRequest request
    ) {
        return studentService.updateStudent(principal, studentId, request);
    }

    @Operation(operationId = "createStudentInvite", summary = "Create an invitation for a student")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student invitation created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found or not owned", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Student already registered or email already in use", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/{studentId}/invites", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentInviteCreatedResponse> createStudentInvite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @Valid @RequestBody CreateStudentInviteRequest request
    ) {
        StudentInviteCreatedResponse response = studentInviteService.createInvite(principal, studentId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/teacher/students/" + studentId + "/invites/" + response.id()
        )).body(response);
    }

    @Operation(operationId = "listStudentInvites", summary = "List invitation metadata for a student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student invitation history"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student not found or not owned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/{studentId}/invites", produces = MediaType.APPLICATION_JSON_VALUE)
    public StudentInviteListResponse listStudentInvites(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId
    ) {
        return studentInviteService.listInvites(principal, studentId);
    }

    @Operation(operationId = "revokeStudentInvite", summary = "Revoke a student invitation")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Student invitation revoked"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Teacher role or CSRF token required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Student or invitation not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Student invitation already accepted", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping(value = "/{studentId}/invites/{inviteId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revokeStudentInvite(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @PathVariable UUID inviteId
    ) {
        studentInviteService.revokeInvite(principal, studentId, inviteId);
        return ResponseEntity.noContent().build();
    }
}
