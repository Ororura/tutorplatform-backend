package com.tutorplatform.program.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface StudentTopicProgressDatabaseRepository
        extends JpaRepository<StudentTopicProgressDatabaseModel, StudentTopicProgressId> {
}
