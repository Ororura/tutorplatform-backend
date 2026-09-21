package com.tutorplatform.report.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProgressReportDatabaseRepository
        extends JpaRepository<ProgressReportDatabaseModel, UUID> {

    Optional<ProgressReportDatabaseModel> findByLearningPeriodId(UUID learningPeriodId);

    boolean existsByLearningPeriodId(UUID learningPeriodId);

    Page<ProgressReportDatabaseModel> findAllByStudentProgramIdOrderByCreatedAtDesc(
            UUID studentProgramId, Pageable pageable);

    Optional<ProgressReportDatabaseModel> findByIdAndStudentProgramIdAndGeneratedByTeacherId(
            UUID id, UUID studentProgramId, UUID generatedByTeacherId);

    @Query(
            value =
                    """
        select report.*
        from progress_reports report
        join student_programs program on program.id = report.student_program_id
        join teacher_student_links ownership
          on ownership.teacher_id = :teacherId
         and ownership.student_id = program.student_id
         and ownership.relation_type = 'PRIMARY'
         and ownership.ended_at is null
        where report.id = :reportId
          and program.assigned_by_teacher_id = :teacherId
        """,
            nativeQuery = true)
    Optional<ProgressReportDatabaseModel> findOwnedById(
            @Param("reportId") UUID reportId, @Param("teacherId") UUID teacherId);
}
