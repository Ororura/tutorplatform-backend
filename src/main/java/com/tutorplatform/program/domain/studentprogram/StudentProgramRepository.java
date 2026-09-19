package com.tutorplatform.program.domain.studentprogram;

import java.util.Optional;
import java.util.UUID;

public interface StudentProgramRepository {
    StudentProgramEntity saveAndFlush(StudentProgramEntity studentProgram);

    Optional<StudentProgramEntity> findById(UUID studentProgramId);

    Optional<StudentProgramEntity> findByIdForUpdate(UUID studentProgramId);

    boolean existsActiveOrPaused(UUID studentId, UUID learningProgramId);

    boolean existsByLearningProgramId(UUID learningProgramId);
}
