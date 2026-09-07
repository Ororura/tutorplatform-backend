package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.user.infrastructure.persistence.UserEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStudentRepository implements StudentRepository {

    private final StudentDatabaseRepository databaseRepository;
    private final EntityManager entityManager;

    public JpaStudentRepository(StudentDatabaseRepository databaseRepository, EntityManager entityManager) {
        this.databaseRepository = databaseRepository;
        this.entityManager = entityManager;
    }

    @Override
    public StudentEntity saveAndFlush(StudentEntity student) {
        StudentDatabaseModel model = databaseRepository.findById(student.getId())
                .orElseGet(() -> new StudentDatabaseModel(student));
        UserEntity user = student.getUserId() == null
                ? null
                : entityManager.getReference(UserEntity.class, student.getUserId());
        model.updateFrom(student, user);
        return databaseRepository.saveAndFlush(model).toEntity();
    }

    @Override
    public Optional<StudentEntity> findById(UUID studentId) {
        return databaseRepository.findById(studentId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findByIdForUpdate(UUID studentId) {
        return databaseRepository.findByIdForUpdate(studentId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findOwnedStudent(UUID teacherId, UUID studentId) {
        return databaseRepository.findOwnedStudent(teacherId, studentId).map(StudentDatabaseModel::toEntity);
    }

    @Override
    public Optional<StudentEntity> findOwnedStudentForUpdate(UUID teacherId, UUID studentId) {
        return databaseRepository.findOwnedStudentForUpdate(teacherId, studentId)
                .map(StudentDatabaseModel::toEntity);
    }

    @Override
    public void deleteAll() {
        databaseRepository.deleteAll();
    }
}
