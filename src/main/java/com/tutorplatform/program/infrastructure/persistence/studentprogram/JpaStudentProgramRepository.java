package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStudentProgramRepository implements StudentProgramRepository {

    private final StudentProgramDatabaseRepository databaseRepository;

    public JpaStudentProgramRepository(StudentProgramDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public StudentProgramEntity saveAndFlush(StudentProgramEntity studentProgram) {
        return databaseRepository.saveAndFlush(new StudentProgramDatabaseModel(studentProgram)).toEntity();
    }

    @Override
    public Optional<StudentProgramEntity> findById(UUID studentProgramId) {
        return databaseRepository.findById(studentProgramId).map(StudentProgramDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentProgramEntity> findByIdForUpdate(UUID studentProgramId) {
        return databaseRepository.findWithLockById(studentProgramId)
            .map(StudentProgramDatabaseModel::toEntity);
    }
}
