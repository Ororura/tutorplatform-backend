package com.tutorplatform.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

interface ModuleDatabaseRepository extends JpaRepository<ModuleDatabaseModel, UUID> {
    @Query("select coalesce(max(module.position), -1) from ModuleDatabaseModel module where module.learningProgramId = :learningProgramId")
    int findMaxPositionByLearningProgramId(UUID learningProgramId);
}
