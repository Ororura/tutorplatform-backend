package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

interface StudentProgramDatabaseRepository extends JpaRepository<StudentProgramDatabaseModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentProgramDatabaseModel> findWithLockById(UUID id);

    @Query("""
        select (count(program) > 0) from StudentProgramDatabaseModel program
        where program.studentId = :studentId
          and program.learningProgramId = :learningProgramId
          and program.status in (com.tutorplatform.program.domain.studentprogram.StudentProgramStatus.ACTIVE,
                                 com.tutorplatform.program.domain.studentprogram.StudentProgramStatus.PAUSED)
        """)
    boolean existsActiveOrPaused(UUID studentId, UUID learningProgramId);
}
