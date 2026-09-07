package com.tutorplatform.program.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudentProgramRepository {
    StudentProgramEntity saveAndFlush(StudentProgramEntity studentProgram);
    Optional<StudentProgramEntity> findById(UUID studentProgramId);
}
