package com.tutorplatform.subject.infrastructure.persistence;

import com.tutorplatform.subject.domain.SubjectStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SubjectDatabaseRepository extends JpaRepository<SubjectDatabaseModel, UUID> {
    @Query(
            """
        select subject from SubjectDatabaseModel subject
        where (subject.ownerTeacherId is null or subject.ownerTeacherId = :teacherId)
          and subject.status = :status
        order by subject.name asc, subject.id asc
        """)
    List<SubjectDatabaseModel> findAccessible(
            @Param("teacherId") UUID teacherId, @Param("status") SubjectStatus status);
}
