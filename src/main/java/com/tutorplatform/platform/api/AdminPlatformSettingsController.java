package com.tutorplatform.platform.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.platform.application.PlatformSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminPlatformSettingsController {

    private final PlatformSettingsService settingsService;

    public AdminPlatformSettingsController(
        PlatformSettingsService settingsService
    ) {
        this.settingsService = settingsService;
    }

    @Operation(
        operationId = "getAdminPlatformSettings",
        summary = "Get platform settings"
    )
    @GetMapping
    public PlatformSettingsResponse getSettings() {

        return PlatformSettingsResponse.from(
            settingsService.getSettings()
        );
    }

    @Operation(
        operationId = "changeRegistrationMode",
        summary = "Change platform registration mode"
    )
    @PatchMapping("/registration")
    public PlatformSettingsResponse changeRegistrationMode(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody ChangeRegistrationModeRequest request
    ) {

        return PlatformSettingsResponse.from(
            settingsService.changeRegistrationMode(
                request.mode(),
                principal.id()
            )
        );
    }
}
