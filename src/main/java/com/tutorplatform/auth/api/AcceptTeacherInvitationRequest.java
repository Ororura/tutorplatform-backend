package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcceptTeacherInvitationRequest(

    @NotBlank
    @Size(min = 2, max = 160)
    @Schema(example = "Егор")
    String displayName,

    @NotNull
    @Size(min = 10, max = 128)
    @Schema(
        example = "correct horse battery staple",
        format = "password"
    )
    String password

) {
    public AcceptTeacherInvitationRequest {
        displayName = displayName == null
            ? null
            : displayName.strip();
    }
}
