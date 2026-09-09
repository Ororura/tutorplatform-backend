package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface HomeworkDatabaseRepository extends JpaRepository<HomeworkDatabaseModel, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<HomeworkDatabaseModel> findWithItemsById(UUID homeworkId);

    @EntityGraph(attributePaths = "items")
    @Query("""
        select distinct homework
        from HomeworkDatabaseModel homework
        join homework.items matchedItem
        where matchedItem.id = :homeworkItemId
        """)
    Optional<HomeworkDatabaseModel> findWithItemsByHomeworkItemId(
        @Param("homeworkItemId") UUID homeworkItemId
    );

    Page<HomeworkDatabaseModel> findAllByStudentProgramIdOrderByAssignedAtDesc(
        UUID studentProgramId,
        Pageable pageable
    );

    @Query("""
        select homework
        from HomeworkDatabaseModel homework
        where homework.assignedByTeacherId = :teacherId
          and homework.studentProgram.studentId = :studentId
          and (:studentProgramId is null or homework.studentProgramId = :studentProgramId)
          and (:status is null or homework.status = :status)
        """)
    Page<HomeworkDatabaseModel> findPageByTeacherAndStudent(
        @Param("teacherId") UUID teacherId,
        @Param("studentId") UUID studentId,
        @Param("studentProgramId") UUID studentProgramId,
        @Param("status") HomeworkStatus status,
        Pageable pageable
    );

    boolean existsByIdAndStudentProgramId(UUID homeworkId, UUID studentProgramId);
}
