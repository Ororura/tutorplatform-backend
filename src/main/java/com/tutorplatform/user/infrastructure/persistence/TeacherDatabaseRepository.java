package com.tutorplatform.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeacherDatabaseRepository extends JpaRepository<TeacherDatabaseModel, UUID> {
    Optional<TeacherDatabaseModel> findByUserId(UUID userId);
}
