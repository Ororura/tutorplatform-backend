package com.tutorplatform.program.infrastructure.persistence.learningprogram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

interface LearningProgramDatabaseRepository extends JpaRepository<LearningProgramDatabaseModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LearningProgramDatabaseModel> findWithLockById(UUID id);
}
