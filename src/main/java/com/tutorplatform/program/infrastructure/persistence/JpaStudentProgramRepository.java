package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.StudentProgramEntity;
import com.tutorplatform.program.domain.StudentProgramRepository;
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
}
