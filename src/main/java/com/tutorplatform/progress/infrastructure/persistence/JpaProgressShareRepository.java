package com.tutorplatform.progress.infrastructure.persistence;

import com.tutorplatform.progress.domain.ProgressShare;
import com.tutorplatform.progress.domain.ProgressShareRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaProgressShareRepository implements ProgressShareRepository {

    private final ProgressShareDatabaseRepository databaseRepository;

    JpaProgressShareRepository(ProgressShareDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public ProgressShare saveAndFlush(ProgressShare share) {
        return databaseRepository.saveAndFlush(new ProgressShareDatabaseModel(share)).toDomain();
    }

    @Override
    public Optional<ProgressShare> findByTokenHash(String tokenHash) {
        return databaseRepository.findByTokenHash(tokenHash).map(ProgressShareDatabaseModel::toDomain);
    }

    @Override
    public List<ProgressShare> findAllOwnedBy(UUID teacherId, UUID studentId, UUID studentProgramId) {
        List<ProgressShareDatabaseModel> shares = studentProgramId == null
            ? databaseRepository.findAllOwnedBy(teacherId, studentId)
            : databaseRepository.findAllOwnedByProgram(teacherId, studentId, studentProgramId);
        return shares.stream().map(ProgressShareDatabaseModel::toDomain).toList();
    }

    @Override
    public Optional<ProgressShare> findOwnedById(UUID shareId, UUID teacherId, UUID studentId) {
        return databaseRepository.findOwnedById(shareId, teacherId, studentId)
            .map(ProgressShareDatabaseModel::toDomain);
    }
}
