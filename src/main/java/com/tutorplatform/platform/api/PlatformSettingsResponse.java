package com.tutorplatform.platform.api;

import com.tutorplatform.platform.domain.PlatformSettings;
import com.tutorplatform.platform.domain.RegistrationMode;
import java.time.Instant;
import java.util.UUID;

public record PlatformSettingsResponse(
        RegistrationMode registrationMode, Instant updatedAt, UUID updatedByAdminId) {

    public static PlatformSettingsResponse from(PlatformSettings settings) {
        return new PlatformSettingsResponse(
                settings.registrationMode(), settings.updatedAt(), settings.updatedByAdminId());
    }
}
