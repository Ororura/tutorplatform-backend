package com.tutorplatform.platform.api;

import com.tutorplatform.platform.domain.RegistrationMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeRegistrationModeRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "INVITE_ONLY")
                RegistrationMode mode) {}
