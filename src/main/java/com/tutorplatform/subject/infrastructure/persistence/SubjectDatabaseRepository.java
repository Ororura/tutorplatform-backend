package com.tutorplatform.subject.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SubjectDatabaseRepository extends JpaRepository<SubjectDatabaseModel, UUID> {
}
