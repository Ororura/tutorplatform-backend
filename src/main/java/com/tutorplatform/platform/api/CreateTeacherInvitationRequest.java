package com.tutorplatform.platform.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeacherInvitationRequest(
    @NotBlank
    @Email
    @Size(max = 320)
    @Schema(example = "teacher@example.com", maxLength = 320)
    String email
) {
    public CreateTeacherInvitationRequest {
        email = email == null ? null : email.strip();
    }
}
