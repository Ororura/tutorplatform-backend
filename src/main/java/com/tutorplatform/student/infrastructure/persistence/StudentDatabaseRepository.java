package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.TeacherStudentRelationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface StudentDatabaseRepository extends JpaRepository<StudentDatabaseModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StudentDatabaseModel> findWithLockById(UUID id);

    @Query("""
        select link.student
        from TeacherStudentLinkEntity link
        where link.teacher.id = :teacherId
          and link.student.id = :studentId
          and link.relationType = :relationType
          and link.endedAt is null
        """)
    Optional<StudentDatabaseModel> findOwnedStudent(UUID teacherId, UUID studentId, TeacherStudentRelationType relationType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select link.student
        from TeacherStudentLinkEntity link
        where link.teacher.id = :teacherId
          and link.student.id = :studentId
          and link.relationType = :relationType
          and link.endedAt is null
        """)
    Optional<StudentDatabaseModel> findOwnedStudentForUpdate(UUID teacherId, UUID studentId, TeacherStudentRelationType relationType);
}
