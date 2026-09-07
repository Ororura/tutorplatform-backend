package com.tutorplatform.student.application;

import com.tutorplatform.student.domain.TeacherStudentRelationType;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.user.domain.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentOwnershipQueryService implements StudentOwnershipQuery {

    private final TeacherRepository teacherRepository;
    private final TeacherStudentLinkRepository teacherStudentLinkRepository;

    public StudentOwnershipQueryService(
        TeacherRepository teacherRepository,
        TeacherStudentLinkRepository teacherStudentLinkRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.teacherStudentLinkRepository = teacherStudentLinkRepository;
    }

    @Override
    public Optional<UUID> findTeacherIdByUserId(UUID userId) {
        return teacherRepository.findByUserId(userId).map(teacher -> teacher.getId());
    }

    @Override
    public boolean isActivePrimaryOwner(UUID teacherId, UUID studentId) {
        return teacherStudentLinkRepository
            .existsByIdTeacherIdAndIdStudentIdAndRelationTypeAndEndedAtIsNull(
                teacherId,
                studentId,
                TeacherStudentRelationType.PRIMARY
            );
    }
}
