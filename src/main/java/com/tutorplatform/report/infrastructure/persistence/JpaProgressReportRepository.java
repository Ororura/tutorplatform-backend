package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportPage;
import com.tutorplatform.report.domain.ProgressReportRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProgressReportRepository implements ProgressReportRepository {

    private final ProgressReportDatabaseRepository databaseRepository;
    private final ProgressReportSnapshotJsonCodec codec;

    JpaProgressReportRepository(
            ProgressReportDatabaseRepository databaseRepository,
            ProgressReportSnapshotJsonCodec codec) {
        this.databaseRepository = databaseRepository;
        this.codec = codec;
    }

    @Override
    public ProgressReport saveAndFlush(ProgressReport report) {
        boolean existing = databaseRepository.existsById(report.id());
        return databaseRepository
                .saveAndFlush(new ProgressReportDatabaseModel(report, codec, existing))
                .toDomain(codec);
    }

    @Override
    public Optional<ProgressReport> findById(UUID id) {
        return databaseRepository.findById(id).map(model -> model.toDomain(codec));
    }

    @Override
    public Optional<ProgressReport> findByLearningPeriodId(UUID learningPeriodId) {
        return databaseRepository
                .findByLearningPeriodId(learningPeriodId)
                .map(model -> model.toDomain(codec));
    }

    @Override
    public boolean existsByLearningPeriodId(UUID learningPeriodId) {
        return databaseRepository.existsByLearningPeriodId(learningPeriodId);
    }

    @Override
    public ProgressReportPage listByStudentProgramId(UUID studentProgramId, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("invalid pagination");
        }
        var result =
                databaseRepository.findAllByStudentProgramIdOrderByCreatedAtDesc(
                        studentProgramId, PageRequest.of(page, size));
        return new ProgressReportPage(
                result.stream().map(model -> model.toDomain(codec)).toList(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public Optional<ProgressReport> findOwnedById(UUID id, UUID studentProgramId, UUID teacherId) {
        return databaseRepository
                .findByIdAndStudentProgramIdAndGeneratedByTeacherId(id, studentProgramId, teacherId)
                .map(model -> model.toDomain(codec));
    }

    @Override
    public Optional<ProgressReport> findOwnedById(UUID id, UUID teacherId) {
        return databaseRepository.findOwnedById(id, teacherId).map(model -> model.toDomain(codec));
    }
}
