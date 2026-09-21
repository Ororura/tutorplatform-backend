package com.tutorplatform.student.api.invite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentInviteRequest(
        @NotBlank @Email @Size(max = 320) @Schema(example = "student@example.com", maxLength = 320)
                String email) {
    public CreateStudentInviteRequest {
        email = email == null ? null : email.strip();
    }
}
