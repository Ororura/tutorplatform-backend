package com.tutorplatform.student.api.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
    @Size(min = 1, max = 100)
    @Schema(example = "Андрей", minLength = 1, maxLength = 100, nullable = true)
    String firstName,

    @Size(min = 1, max = 100)
    @Schema(example = "Петров", minLength = 1, maxLength = 100, nullable = true)
    String lastName
) {
    public UpdateStudentRequest {
        firstName = firstName == null ? null : firstName.strip();
        lastName = lastName == null ? null : lastName.strip();
    }

    @AssertTrue(message = "at least one of firstName or lastName must be provided")
    @JsonIgnore
    public boolean isAnyFieldProvided() {
        return firstName != null || lastName != null;
    }
}
