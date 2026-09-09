package com.tutorplatform.student.application;

import com.tutorplatform.student.domain.TeacherStudentRelationType;
import com.tutorplatform.student.domain.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final TeacherStudentLinkRepository teacherStudentLinkRepository;

    public StudentOwnershipQueryService(
        TeacherRepository teacherRepository,
        StudentRepository studentRepository,
        TeacherStudentLinkRepository teacherStudentLinkRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.teacherStudentLinkRepository = teacherStudentLinkRepository;
    }

    @Override
    public Optional<UUID> findTeacherIdByUserId(UUID userId) {
        return teacherRepository.findByUserId(userId).map(teacher -> teacher.id());
    }

    @Override
    public Optional<UUID> findStudentIdByUserId(UUID userId) {
        return studentRepository.findByUserId(userId).map(student -> student.getId());
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
