package com.tutorplatform.subject.infrastructure.persistence;

import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class JpaSubjectRepository implements SubjectRepository {

    private final SubjectDatabaseRepository databaseRepository;

    JpaSubjectRepository(SubjectDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public SubjectEntity saveAndFlush(SubjectEntity subject) {
        SubjectDatabaseModel model = databaseRepository.findById(subject.id())
            .orElseGet(() -> new SubjectDatabaseModel(subject));
        model.updateFrom(subject);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<SubjectEntity> findById(UUID subjectId) {
        return databaseRepository.findById(subjectId).map(SubjectDatabaseModel::toEntity);
    }

    @Override
    public List<SubjectEntity> findAccessibleByTeacherAndStatus(UUID teacherId, com.tutorplatform.subject.domain.SubjectStatus status) {
        return databaseRepository.findAccessible(teacherId, status).stream()
            .map(SubjectDatabaseModel::toEntity)
            .toList();
    }
}
