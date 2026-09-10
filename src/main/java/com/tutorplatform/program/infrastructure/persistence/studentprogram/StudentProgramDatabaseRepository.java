package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

interface StudentProgramDatabaseRepository extends JpaRepository<StudentProgramDatabaseModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentProgramDatabaseModel> findWithLockById(UUID id);
}
