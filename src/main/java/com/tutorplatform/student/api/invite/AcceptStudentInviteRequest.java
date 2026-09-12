package com.tutorplatform.student.api.invite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AcceptStudentInviteRequest(
    @NotNull
    @Size(min = 10, max = 128)
    @Schema(example = "correct horse battery staple", minLength = 10, maxLength = 128, format = "password")
    String password
) {
}
