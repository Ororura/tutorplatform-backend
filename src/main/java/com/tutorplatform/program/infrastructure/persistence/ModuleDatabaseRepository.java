package com.tutorplatform.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ModuleDatabaseRepository extends JpaRepository<ModuleDatabaseModel, UUID> {
}
