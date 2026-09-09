package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.submission.domain.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SubmissionDatabaseRepository extends JpaRepository<SubmissionDatabaseModel, UUID> {

    Optional<SubmissionDatabaseModel> findByIdAndStudentId(UUID submissionId, UUID studentId);

    Page<SubmissionDatabaseModel> findAllByStudentId(UUID studentId, Pageable pageable);

    Page<SubmissionDatabaseModel> findAllByStudentIdAndTaskId(
            UUID studentId,
            UUID taskId,
            Pageable pageable
    );

    Page<SubmissionDatabaseModel> findAllByStudentIdAndStatus(
            UUID studentId,
            SubmissionStatus status,
            Pageable pageable
    );

    @Query("""
        select submission
        from SubmissionDatabaseModel submission
        where submission.studentId = :studentId
          and submission.studentProgramId = :studentProgramId
          and submission.taskId = :taskId
          and ((:homeworkItemId is null and submission.homeworkItemId is null)
               or submission.homeworkItemId = :homeworkItemId)
        """)
    Page<SubmissionDatabaseModel> findAttempts(
            @Param("studentId") UUID studentId,
            @Param("studentProgramId") UUID studentProgramId,
            @Param("taskId") UUID taskId,
            @Param("homeworkItemId") UUID homeworkItemId,
            Pageable pageable
    );

    @Query("""
        select submission
        from SubmissionDatabaseModel submission
        where submission.studentId = :studentId
          and submission.studentProgramId = :studentProgramId
          and submission.taskId = :taskId
          and ((:homeworkItemId is null and submission.homeworkItemId is null)
               or submission.homeworkItemId = :homeworkItemId)
        order by submission.attemptNo desc
        limit 1
        """)
    Optional<SubmissionDatabaseModel> findLatestAttempt(
            @Param("studentId") UUID studentId,
            @Param("studentProgramId") UUID studentProgramId,
            @Param("taskId") UUID taskId,
            @Param("homeworkItemId") UUID homeworkItemId
    );

    @Query("""
        select (count(submission) > 0)
        from SubmissionDatabaseModel submission
        where submission.studentId = :studentId
          and submission.studentProgramId = :studentProgramId
          and submission.taskId = :taskId
          and ((:homeworkItemId is null and submission.homeworkItemId is null)
               or submission.homeworkItemId = :homeworkItemId)
          and submission.status = :status
        """)
    boolean existsByStatus(
            @Param("studentId") UUID studentId,
            @Param("studentProgramId") UUID studentProgramId,
            @Param("taskId") UUID taskId,
            @Param("homeworkItemId") UUID homeworkItemId,
            @Param("status") SubmissionStatus status
    );

    @Query("""
        select coalesce(max(submission.attemptNo), 0)
        from SubmissionDatabaseModel submission
        where submission.studentId = :studentId
          and submission.studentProgramId = :studentProgramId
          and submission.taskId = :taskId
          and ((:homeworkItemId is null and submission.homeworkItemId is null)
               or submission.homeworkItemId = :homeworkItemId)
        """)
    int findMaxAttemptNo(
            @Param("studentId") UUID studentId,
            @Param("studentProgramId") UUID studentProgramId,
            @Param("taskId") UUID taskId,
            @Param("homeworkItemId") UUID homeworkItemId
    );

    @Query(value = "select id from student_programs where id = :studentProgramId for update", nativeQuery = true)
    UUID lockStudentProgram(@Param("studentProgramId") UUID studentProgramId);
}
