package com.tutorplatform.platform.domain;

import java.time.Instant;
import java.util.UUID;

public record PlatformSettings(
        RegistrationMode registrationMode, Instant updatedAt, UUID updatedByAdminId) {}
