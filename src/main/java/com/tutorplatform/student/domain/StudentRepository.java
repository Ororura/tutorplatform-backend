package com.tutorplatform.student.domain;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

    StudentEntity saveAndFlush(StudentEntity student);

    Optional<StudentEntity> findById(UUID studentId);

    Optional<StudentEntity> findByIdForUpdate(UUID studentId);

    Optional<StudentEntity> findOwnedStudent(UUID teacherId, UUID studentId);

    Optional<StudentEntity> findOwnedStudentForUpdate(UUID teacherId, UUID studentId);

    void deleteAll();
}
