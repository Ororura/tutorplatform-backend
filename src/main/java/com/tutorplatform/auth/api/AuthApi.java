package com.tutorplatform.auth.api;

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

public interface AuthApi {
    @Operation(operationId = "registerTeacher", summary = "Register a teacher account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Teacher registered"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Missing or invalid CSRF token",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Email already registered",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CurrentUserResponse registerTeacher(
            TeacherRegistrationRequest registration,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response);

    @Operation(operationId = "login", summary = "Log in with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Missing or invalid CSRF token",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CurrentUserResponse login(
            LoginRequest login,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response);

    @Operation(operationId = "getCurrentUser", summary = "Get the current authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    CurrentUserResponse getCurrentUser(@Parameter(hidden = true) AuthenticatedUser principal);

    @Operation(operationId = "logout", summary = "Log out and invalidate the current session")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out"),
        @ApiResponse(
                responseCode = "403",
                description = "Missing or invalid CSRF token",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    void logout();
}
