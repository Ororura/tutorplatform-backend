package com.tutorplatform.student.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface StudentDatabaseRepository extends JpaRepository<StudentDatabaseModel, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from StudentDatabaseModel student where student.id = :studentId")
    Optional<StudentDatabaseModel> findByIdForUpdate(@Param("studentId") UUID studentId);

    @Query("""
            select link.student
            from TeacherStudentLinkEntity link
            where link.teacher.id = :teacherId
              and link.student.id = :studentId
              and link.relationType = com.tutorplatform.student.domain.TeacherStudentRelationType.PRIMARY
              and link.endedAt is null
            """)
    Optional<StudentDatabaseModel> findOwnedStudent(
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId
    );

    @Query(value = """
            SELECT s.*
            FROM students s
            WHERE s.id = :studentId
              AND EXISTS (
                  SELECT 1
                  FROM teacher_student_links link
                  WHERE link.teacher_id = :teacherId
                    AND link.student_id = s.id
                    AND link.relation_type = 'PRIMARY'
                    AND link.ended_at IS NULL
              )
            FOR UPDATE
            """, nativeQuery = true)
    Optional<StudentDatabaseModel> findOwnedStudentForUpdate(
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId
    );
}
