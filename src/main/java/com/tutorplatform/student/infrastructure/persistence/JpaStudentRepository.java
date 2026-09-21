package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.TeacherStudentRelationType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaStudentRepository implements StudentRepository {

    private final StudentDatabaseRepository databaseRepository;

    JpaStudentRepository(StudentDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public StudentEntity saveAndFlush(StudentEntity student) {
        StudentDatabaseModel model =
                databaseRepository
                        .findById(student.getId())
                        .orElseGet(() -> new StudentDatabaseModel(student));
        model.updateFrom(student);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<StudentEntity> findById(UUID studentId) {
        return databaseRepository.findById(studentId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findByUserId(UUID userId) {
        return databaseRepository.findByUserId(userId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findByIdForUpdate(UUID studentId) {
        return databaseRepository.findWithLockById(studentId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findOwnedStudent(UUID teacherId, UUID studentId) {
        return databaseRepository
                .findOwnedStudent(teacherId, studentId, TeacherStudentRelationType.PRIMARY)
                .map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findOwnedStudentForUpdate(UUID teacherId, UUID studentId) {
        return databaseRepository
                .findOwnedStudentForUpdate(teacherId, studentId, TeacherStudentRelationType.PRIMARY)
                .map(StudentDatabaseModel::toEntity);
    }

    @Override
    public void deleteAll() {
        databaseRepository.deleteAll();
    }
}
