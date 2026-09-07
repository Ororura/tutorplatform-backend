package com.tutorplatform.session.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface LessonSessionDatabaseRepository extends JpaRepository<LessonSessionDatabaseModel, UUID> {
}
