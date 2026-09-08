package com.tutorplatform.task.infrastructure.persistence.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SkillDatabaseRepository extends JpaRepository<SkillDatabaseModel, UUID> {
    Optional<SkillDatabaseModel> findBySubjectIdAndCode(UUID subjectId, String code);
}
