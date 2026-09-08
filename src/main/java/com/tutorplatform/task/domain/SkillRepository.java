package com.tutorplatform.task.domain;

import java.util.Optional;
import java.util.UUID;

public interface SkillRepository {
    SkillEntity saveAndFlush(SkillEntity skill);
    Optional<SkillEntity> findById(UUID skillId);
    Optional<SkillEntity> findBySubjectIdAndCode(UUID subjectId, String code);
}
