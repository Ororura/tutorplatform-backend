package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Email
    @Size(max = 320)
    @Schema(example = "teacher@example.com", maxLength = 320)
    String email,

    @NotNull
    @Size(min = 10, max = 128)
    @Schema(example = "correct horse battery staple", minLength = 10, maxLength = 128, format = "password")
    String password
) {
    public LoginRequest {
        email = email == null ? null : email.strip();
    }
}
