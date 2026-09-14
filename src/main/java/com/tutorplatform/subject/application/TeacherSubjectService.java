package com.tutorplatform.subject.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.subject.api.SubjectSummaryResponse;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherSubjectService {

    private final StudentOwnershipQuery ownershipQuery;
    private final SubjectRepository subjectRepository;

    public TeacherSubjectService(StudentOwnershipQuery ownershipQuery, SubjectRepository subjectRepository) {
        this.ownershipQuery = ownershipQuery;
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectSummaryResponse> list(AuthenticatedUser principal, SubjectStatus status) {
        UUID teacherId = ownershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
        return subjectRepository.findAccessibleByTeacherAndStatus(teacherId, status).stream()
            .map(subject -> new SubjectSummaryResponse(
                subject.id(), subject.code(), subject.name(), subject.description(), subject.status()
            ))
            .toList();
    }
}
