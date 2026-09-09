package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStudentTopicProgressRepository implements StudentTopicProgressRepository {

    private final StudentTopicProgressDatabaseRepository databaseRepository;

    public JpaStudentTopicProgressRepository(StudentTopicProgressDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public StudentTopicProgressEntity saveAndFlush(StudentTopicProgressEntity progress) {
        StudentTopicProgressId id = new StudentTopicProgressId(
            progress.studentProgramId(), progress.topicId()
        );
        StudentTopicProgressDatabaseModel model = databaseRepository.findById(id)
            .orElseGet(() -> new StudentTopicProgressDatabaseModel(progress));
        model.updateFrom(progress);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<StudentTopicProgressEntity> findById(UUID studentProgramId, UUID topicId) {
        return databaseRepository.findById(new StudentTopicProgressId(studentProgramId, topicId))
            .map(StudentTopicProgressDatabaseModel::toEntity);
    }
}
