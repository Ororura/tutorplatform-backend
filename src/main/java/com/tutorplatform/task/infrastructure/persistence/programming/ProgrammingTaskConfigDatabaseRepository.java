package com.tutorplatform.task.infrastructure.persistence.programming;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProgrammingTaskConfigDatabaseRepository
        extends JpaRepository<ProgrammingTaskConfigDatabaseModel, UUID> {}
