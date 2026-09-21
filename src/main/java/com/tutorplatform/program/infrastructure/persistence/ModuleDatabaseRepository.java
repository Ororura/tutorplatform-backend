package com.tutorplatform.program.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface ModuleDatabaseRepository extends JpaRepository<ModuleDatabaseModel, UUID> {
    List<ModuleDatabaseModel> findByLearningProgramId(UUID learningProgramId);

    @Query(
            "select coalesce(max(module.position), -1) from ModuleDatabaseModel module where module.learningProgramId = :learningProgramId")
    int findMaxPositionByLearningProgramId(UUID learningProgramId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
        update ModuleDatabaseModel module
        set module.position = :position
        where module.learningProgramId = :learningProgramId and module.id = :moduleId
        """)
    int updatePosition(UUID learningProgramId, UUID moduleId, int position);
}
