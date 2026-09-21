package com.tutorplatform.platform.api;

import com.tutorplatform.platform.application.PlatformSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/registration-settings")
public class PublicRegistrationController {

    private final PlatformSettingsService settingsService;

    public PublicRegistrationController(PlatformSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(
            operationId = "getPublicRegistrationSettings",
            summary = "Get public registration settings")
    @GetMapping
    public RegistrationSettingsResponse getSettings() {

        var settings = settingsService.getSettings();

        return new RegistrationSettingsResponse(settings.registrationMode());
    }
}
