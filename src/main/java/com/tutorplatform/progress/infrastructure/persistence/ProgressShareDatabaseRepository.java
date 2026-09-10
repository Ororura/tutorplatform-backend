package com.tutorplatform.progress.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProgressShareDatabaseRepository extends JpaRepository<ProgressShareDatabaseModel, UUID> {

    Optional<ProgressShareDatabaseModel> findByTokenHash(String tokenHash);

    @Query(value = """
        SELECT share.*
        FROM progress_shares share
        JOIN student_programs program ON program.id = share.student_program_id
        WHERE share.created_by_teacher_id = :teacherId
          AND program.student_id = :studentId
        ORDER BY share.created_at DESC
        """, nativeQuery = true)
    List<ProgressShareDatabaseModel> findAllOwnedBy(
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId
    );

    @Query(value = """
        SELECT share.*
        FROM progress_shares share
        JOIN student_programs program ON program.id = share.student_program_id
        WHERE share.created_by_teacher_id = :teacherId
          AND program.student_id = :studentId
          AND share.student_program_id = :studentProgramId
        ORDER BY share.created_at DESC
        """, nativeQuery = true)
    List<ProgressShareDatabaseModel> findAllOwnedByProgram(
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId,
        @Param("studentProgramId") UUID studentProgramId
    );

    @Query(value = """
        SELECT share.*
        FROM progress_shares share
        JOIN student_programs program ON program.id = share.student_program_id
        WHERE share.id = :shareId
          AND share.created_by_teacher_id = :teacherId
          AND program.student_id = :studentId
        """, nativeQuery = true)
    Optional<ProgressShareDatabaseModel> findOwnedById(
        @Param("shareId") UUID shareId,
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId
    );
}
