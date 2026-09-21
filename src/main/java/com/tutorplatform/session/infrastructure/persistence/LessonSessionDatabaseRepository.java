package com.tutorplatform.session.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LessonSessionDatabaseRepository extends JpaRepository<LessonSessionDatabaseModel, UUID> {

    @Query(
            """
        select lessonSession
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.studentProgramId = :studentProgramId
          and lessonSession.attendanceStatus = com.tutorplatform.session.domain.AttendanceStatus.ATTENDED
        order by lessonSession.startedAt, lessonSession.id
        """)
    List<LessonSessionDatabaseModel> findAttendedByStudentProgram(
            @Param("studentProgramId") UUID studentProgramId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LessonSessionDatabaseModel> findWithLockById(UUID id);

    @Query(
            """
        select lessonSession.studentProgram.studentId
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.id = :lessonSessionId
        """)
    Optional<UUID> findStudentIdById(@Param("lessonSessionId") UUID lessonSessionId);

    @Query(
            """
        select lessonSession
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.id = :lessonSessionId
          and lessonSession.teacherId = :teacherId
          and lessonSession.studentProgram.studentId = :studentId
        """)
    Optional<LessonSessionDatabaseModel> findOwnedById(
            @Param("lessonSessionId") UUID lessonSessionId,
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId);

    @Query(
            """
        select lessonSession
        from LessonSessionDatabaseModel lessonSession
        where lessonSession.teacherId = :teacherId
          and lessonSession.studentProgram.studentId = :studentId
        """)
    Page<LessonSessionDatabaseModel> findPageByTeacherAndStudent(
            @Param("teacherId") UUID teacherId,
            @Param("studentId") UUID studentId,
            Pageable pageable);
}
