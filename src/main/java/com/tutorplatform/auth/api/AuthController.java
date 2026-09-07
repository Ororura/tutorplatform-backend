package com.tutorplatform.auth.api;

import com.tutorplatform.auth.application.AuthenticationSessionService;
import com.tutorplatform.auth.application.CurrentUserService;
import com.tutorplatform.auth.application.TeacherRegistrationService;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TeacherRegistrationService teacherRegistrationService;
    private final AuthenticationSessionService authenticationSessionService;
    private final CurrentUserService currentUserService;

    public AuthController(
            TeacherRegistrationService teacherRegistrationService,
            AuthenticationSessionService authenticationSessionService,
            CurrentUserService currentUserService
    ) {
        this.teacherRegistrationService = teacherRegistrationService;
        this.authenticationSessionService = authenticationSessionService;
        this.currentUserService = currentUserService;
    }

    @Operation(operationId = "registerTeacher", summary = "Register a teacher account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Teacher registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/register/teacher", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CurrentUserResponse registerTeacher(
            @Valid @RequestBody TeacherRegistrationRequest registration,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        teacherRegistrationService.registerTeacher(registration);
        return authenticationSessionService.authenticate(
                registration.email(), registration.password(), request, response
        );
    }

    @Operation(operationId = "login", summary = "Log in with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse login(
            @Valid @RequestBody LoginRequest login,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        return authenticationSessionService.authenticate(login.email(), login.password(), request, response);
    }

    @Operation(operationId = "getCurrentUser", summary = "Get the current authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return currentUserService.getCurrentUser(principal);
    }

    @Operation(operationId = "logout", summary = "Log out and invalidate the current session")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid CSRF token", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Spring Security's LogoutFilter handles this path before controller dispatch.
    }
}
