package com.tutorplatform.auth.api;

import com.tutorplatform.auth.application.AuthenticationSessionService;
import com.tutorplatform.auth.application.CurrentUserService;
import com.tutorplatform.auth.application.TeacherRegistrationService;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {

    private final TeacherRegistrationService teacherRegistrationService;
    private final AuthenticationSessionService authenticationSessionService;
    private final CurrentUserService currentUserService;

    public AuthController(
            TeacherRegistrationService teacherRegistrationService,
            AuthenticationSessionService authenticationSessionService,
            CurrentUserService currentUserService) {
        this.teacherRegistrationService = teacherRegistrationService;
        this.authenticationSessionService = authenticationSessionService;
        this.currentUserService = currentUserService;
    }

    @Override
    @PostMapping(
            value = "/register/teacher",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CurrentUserResponse registerTeacher(
            @Valid @RequestBody TeacherRegistrationRequest registration,
            HttpServletRequest request,
            HttpServletResponse response) {
        teacherRegistrationService.registerTeacher(registration);
        return authenticationSessionService.authenticate(
                registration.email(), registration.password(), request, response);
    }

    @Override
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse login(
            @Valid @RequestBody LoginRequest login,
            HttpServletRequest request,
            HttpServletResponse response) {
        return authenticationSessionService.authenticate(
                login.email(), login.password(), request, response);
    }

    @Override
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return currentUserService.getCurrentUser(principal);
    }

    @Override
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Spring Security's LogoutFilter handles this path before controller dispatch.
    }
}
