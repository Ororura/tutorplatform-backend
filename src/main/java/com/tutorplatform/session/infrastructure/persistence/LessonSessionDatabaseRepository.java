package com.tutorplatform.session.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface LessonSessionDatabaseRepository extends JpaRepository<LessonSessionDatabaseModel, UUID> {

    @Query("""
        select lessonSession
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.id = :lessonSessionId
          and lessonSession.teacherId = :teacherId
          and lessonSession.studentProgram.studentId = :studentId
        """)
    Optional<LessonSessionDatabaseModel> findOwnedById(
        @Param("lessonSessionId") UUID lessonSessionId,
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId
    );

    @Query("""
        select lessonSession
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.teacherId = :teacherId
          and lessonSession.studentProgram.studentId = :studentId
        """)
    Page<LessonSessionDatabaseModel> findPageByTeacherAndStudent(
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId,
        Pageable pageable
    );
}
