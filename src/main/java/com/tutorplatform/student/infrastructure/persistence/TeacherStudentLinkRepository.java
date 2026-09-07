package com.tutorplatform.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TeacherStudentLinkRepository
        extends JpaRepository<TeacherStudentLinkEntity, TeacherStudentLinkId> {

    @Query(value = """
            SELECT EXISTS(
                SELECT 1
                FROM teacher_student_links
                WHERE teacher_id = :teacherId
                  AND student_id = :studentId
                  AND relation_type = 'PRIMARY'
                  AND ended_at IS NULL
            )
            """, nativeQuery = true)
    boolean existsActivePrimaryLink(
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId
    );
}
