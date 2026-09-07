package com.tutorplatform.program.application;

import java.util.Optional;
import java.util.UUID;

public interface ProgramQuery {

    Optional<StudentProgramContext> findStudentProgram(UUID studentProgramId);

    boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId);

    record StudentProgramContext(
        UUID id,
        UUID studentId,
        UUID learningProgramId,
        UUID assignedByTeacherId
    ) {
        public boolean belongsToStudent(UUID expectedStudentId) {
            return studentId.equals(expectedStudentId);
        }

        public boolean isAssignedBy(UUID teacherId) {
            return assignedByTeacherId.equals(teacherId);
        }
    }
}
