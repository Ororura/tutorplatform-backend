package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.TeacherStudentRelationType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherStudentLinkRepository
        extends JpaRepository<TeacherStudentLinkEntity, TeacherStudentLinkId> {

    boolean existsByIdTeacherIdAndIdStudentIdAndRelationTypeAndEndedAtIsNull(
            UUID teacherId, UUID studentId, TeacherStudentRelationType relationType);
}
