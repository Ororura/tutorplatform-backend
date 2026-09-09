package com.tutorplatform.task.infrastructure.persistence.programming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProgrammingTaskConfigDatabaseRepository
    extends JpaRepository<ProgrammingTaskConfigDatabaseModel, UUID> {
}
