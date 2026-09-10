package com.tutorplatform.program.application;

import java.util.Optional;
import java.util.UUID;

public interface ProgramQuery {

    Optional<StudentProgramContext> findStudentProgram(UUID studentProgramId);

    Optional<StudentProgramContext> findStudentProgramForUpdate(UUID studentProgramId);

    Optional<TopicContext> findTopic(UUID topicId);

    boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId);

    record TopicContext(
        UUID id,
        UUID learningProgramId,
        UUID teacherId,
        UUID subjectId
    ) {
        public boolean isOwnedBy(UUID expectedTeacherId) {
            return teacherId.equals(expectedTeacherId);
        }
    }

    record StudentProgramContext(
        UUID id,
        UUID studentId,
        UUID learningProgramId,
        UUID assignedByTeacherId,
        UUID subjectId,
        int reportIntervalMinutes
    ) {
        public boolean belongsToStudent(UUID expectedStudentId) {
            return studentId.equals(expectedStudentId);
        }

        public boolean isAssignedBy(UUID teacherId) {
            return assignedByTeacherId.equals(teacherId);
        }
    }
}
