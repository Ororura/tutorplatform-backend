package com.tutorplatform.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TeacherDatabaseRepository extends JpaRepository<TeacherDatabaseModel, UUID> {
    Optional<TeacherDatabaseModel> findByUserId(UUID userId);
}
