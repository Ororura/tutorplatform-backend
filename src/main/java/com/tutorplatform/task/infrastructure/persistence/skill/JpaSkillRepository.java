package com.tutorplatform.task.infrastructure.persistence.skill;

import com.tutorplatform.task.domain.skill.SkillEntity;
import com.tutorplatform.task.domain.skill.SkillRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSkillRepository implements SkillRepository {

    private final SkillDatabaseRepository databaseRepository;

    JpaSkillRepository(SkillDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public SkillEntity saveAndFlush(SkillEntity skill) {
        return databaseRepository.saveAndFlush(new SkillDatabaseModel(skill)).toEntity();
    }

    @Override
    public Optional<SkillEntity> findById(UUID skillId) {
        return databaseRepository.findById(skillId).map(SkillDatabaseModel::toEntity);
    }

    @Override
    public Optional<SkillEntity> findBySubjectIdAndCode(UUID subjectId, String code) {
        return databaseRepository
                .findBySubjectIdAndCode(subjectId, code)
                .map(SkillDatabaseModel::toEntity);
    }
}
