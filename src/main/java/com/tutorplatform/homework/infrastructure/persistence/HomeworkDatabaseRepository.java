package com.tutorplatform.homework.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface HomeworkDatabaseRepository extends JpaRepository<HomeworkDatabaseModel, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<HomeworkDatabaseModel> findWithItemsById(UUID homeworkId);

    Page<HomeworkDatabaseModel> findAllByStudentProgramIdOrderByAssignedAtDesc(
            UUID studentProgramId,
            Pageable pageable
    );

    boolean existsByIdAndStudentProgramId(UUID homeworkId, UUID studentProgramId);
}
