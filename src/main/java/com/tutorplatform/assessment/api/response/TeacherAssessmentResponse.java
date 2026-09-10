package com.tutorplatform.assessment.api.response;

import com.tutorplatform.assessment.application.TeacherAssessmentResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TeacherAssessmentResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") UUID lessonSessionId,
    @Schema(nullable = true, minimum = "1", maximum = "5") Integer understandingScore,
    @Schema(nullable = true, minimum = "1", maximum = "5") Integer independenceScore,
    @Schema(nullable = true, minimum = "1", maximum = "5") Integer practiceScore,
    @Schema(nullable = true, minimum = "1", maximum = "5") Integer homeworkScore,
    @Schema(nullable = true) String publicComment,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant createdAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant updatedAt
) {
    public static TeacherAssessmentResponse from(TeacherAssessmentResult assessment) {
        return new TeacherAssessmentResponse(
            assessment.id(), assessment.lessonSessionId(), assessment.understandingScore(),
            assessment.independenceScore(), assessment.practiceScore(), assessment.homeworkScore(),
            assessment.publicComment(), assessment.createdAt(), assessment.updatedAt()
        );
    }
}
