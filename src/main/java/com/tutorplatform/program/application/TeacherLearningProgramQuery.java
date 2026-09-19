package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherLearningProgramQuery {
    List<LearningProgramSummary> findPrograms(UUID teacherId, LearningProgramStatus status);

    Optional<LearningProgramDetails> findProgram(UUID teacherId, UUID learningProgramId);

    List<UUID> findTopicIds(UUID learningProgramId);

    record LearningProgramSummary(
        UUID id, UUID subjectId, String subjectCode, String subjectName, String title,
        String description, LearningProgramStatus status, Instant createdAt, Instant updatedAt
    ) {
    }

    record LearningProgramDetails(
        UUID id, UUID subjectId, String subjectCode, String subjectName, String title,
        String description, LearningProgramStatus status, Long version, Instant createdAt, Instant updatedAt,
        boolean hasAssignments, List<ModuleDetails> modules
    ) {
    }

    record ModuleDetails(UUID id, String title, String description, int position, List<TopicDetails> topics) {
    }

    record TopicDetails(
        UUID id, String title, String description, int position,
        com.tutorplatform.program.domain.TopicStatus status, Long version
    ) {
    }
}
