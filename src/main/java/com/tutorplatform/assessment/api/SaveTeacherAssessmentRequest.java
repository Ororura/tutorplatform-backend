package com.tutorplatform.assessment.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SaveTeacherAssessmentRequest(
        @Min(1) @Max(5) @Schema(nullable = true, minimum = "1", maximum = "5")
                Integer understandingScore,
        @Min(1) @Max(5) @Schema(nullable = true, minimum = "1", maximum = "5")
                Integer independenceScore,
        @Min(1) @Max(5) @Schema(nullable = true, minimum = "1", maximum = "5")
                Integer practiceScore,
        @Min(1) @Max(5) @Schema(nullable = true, minimum = "1", maximum = "5")
                Integer homeworkScore,
        @Schema(nullable = true) String publicComment) {}
