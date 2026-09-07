package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.TeacherStudentRelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TeacherStudentLinkRepository
    extends JpaRepository<TeacherStudentLinkEntity, TeacherStudentLinkId> {

    boolean existsByIdTeacherIdAndIdStudentIdAndRelationTypeAndEndedAtIsNull(
        UUID teacherId,
        UUID studentId,
        TeacherStudentRelationType relationType
    );
}
