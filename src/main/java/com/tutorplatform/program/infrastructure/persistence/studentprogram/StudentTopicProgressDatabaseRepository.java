package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import org.springframework.data.jpa.repository.JpaRepository;

interface StudentTopicProgressDatabaseRepository
        extends JpaRepository<StudentTopicProgressDatabaseModel, StudentTopicProgressId> {}
