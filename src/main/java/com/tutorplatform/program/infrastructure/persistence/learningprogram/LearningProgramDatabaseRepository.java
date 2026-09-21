package com.tutorplatform.program.infrastructure.persistence.learningprogram;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface LearningProgramDatabaseRepository
        extends JpaRepository<LearningProgramDatabaseModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LearningProgramDatabaseModel> findWithLockById(UUID id);
}
