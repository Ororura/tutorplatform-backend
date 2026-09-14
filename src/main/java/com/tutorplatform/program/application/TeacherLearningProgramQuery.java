package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TeacherLearningProgramQuery {
    List<LearningProgramSummary> findPrograms(UUID teacherId, LearningProgramStatus status);

    List<UUID> findTopicIds(UUID learningProgramId);

    record LearningProgramSummary(
        UUID id, UUID subjectId, String subjectCode, String subjectName, String title,
        String description, LearningProgramStatus status, Instant createdAt, Instant updatedAt
    ) {
    }
}
