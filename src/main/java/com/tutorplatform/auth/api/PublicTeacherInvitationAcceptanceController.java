package com.tutorplatform.auth.api;

import com.tutorplatform.auth.application.AuthenticationSessionService;
import com.tutorplatform.auth.application.TeacherRegistrationService;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/teacher-invitations")
public class PublicTeacherInvitationAcceptanceController {

    private final TeacherRegistrationService registrationService;
    private final AuthenticationSessionService sessionService;

    public PublicTeacherInvitationAcceptanceController(
            TeacherRegistrationService registrationService,
            AuthenticationSessionService sessionService) {
        this.registrationService = registrationService;
        this.sessionService = sessionService;
    }

    @Operation(
            operationId = "acceptTeacherInvitation",
            summary = "Register a teacher using an invitation")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Teacher registered and authenticated"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Invalid CSRF token",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Invitation not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Invitation unavailable or email registered",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(
            value = "/{token}/accept",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentUserResponse> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptTeacherInvitationRequest registration,
            HttpServletRequest request,
            HttpServletResponse response) {
        String email =
                registrationService.registerInvitedTeacher(
                        token, registration.displayName(), registration.password());

        CurrentUserResponse currentUser =
                sessionService.authenticate(email, registration.password(), request, response);

        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(currentUser);
    }
}
