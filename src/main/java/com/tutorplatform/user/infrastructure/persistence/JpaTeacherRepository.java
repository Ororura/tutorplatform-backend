package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTeacherRepository implements TeacherRepository {
    private final TeacherDatabaseRepository databaseRepository;

    JpaTeacherRepository(TeacherDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override public TeacherEntity save(TeacherEntity teacher) { return saveModel(teacher, false); }
    @Override public TeacherEntity saveAndFlush(TeacherEntity teacher) { return saveModel(teacher, true); }

    private TeacherEntity saveModel(TeacherEntity teacher, boolean flush) {
        TeacherDatabaseModel model = databaseRepository.findById(teacher.getId())
                .orElseGet(() -> new TeacherDatabaseModel(teacher));
        model.updateFrom(teacher);
        model = flush ? databaseRepository.saveAndFlush(model) : databaseRepository.save(model);
        return model.toEntity();
    }

    @Override public Optional<TeacherEntity> findByUserId(UUID userId) {
        return databaseRepository.findByUserId(userId).map(TeacherDatabaseModel::toEntity);
    }
    @Override public long count() { return databaseRepository.count(); }
    @Override public void deleteAll() { databaseRepository.deleteAll(); }
}
