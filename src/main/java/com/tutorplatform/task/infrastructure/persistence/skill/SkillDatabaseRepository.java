package com.tutorplatform.task.infrastructure.persistence.skill;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SkillDatabaseRepository extends JpaRepository<SkillDatabaseModel, UUID> {
    Optional<SkillDatabaseModel> findBySubjectIdAndCode(UUID subjectId, String code);
}
