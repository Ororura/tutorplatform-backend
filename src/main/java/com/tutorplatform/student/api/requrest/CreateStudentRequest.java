package com.tutorplatform.student.api.requrest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
    @NotBlank
    @Size(max = 100)
    @Schema(example = "Андрей", minLength = 1, maxLength = 100)
    String firstName,

    @Size(min = 1, max = 100)
    @Schema(example = "Иванов", minLength = 1, maxLength = 100, nullable = true)
    String lastName
) {
    public CreateStudentRequest {
        firstName = firstName == null ? null : firstName.strip();
        lastName = lastName == null ? null : lastName.strip();
    }
}
