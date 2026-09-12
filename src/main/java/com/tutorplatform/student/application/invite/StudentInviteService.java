package com.tutorplatform.student.application.invite;

import com.tutorplatform.auth.application.EmailAlreadyRegisteredException;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.api.invite.CreateStudentInviteRequest;
import com.tutorplatform.student.api.invite.StudentInviteCreatedResponse;
import com.tutorplatform.student.api.invite.StudentInviteListResponse;
import com.tutorplatform.student.api.invite.StudentInviteSummaryResponse;
import com.tutorplatform.student.application.exception.StudentAlreadyRegisteredException;
import com.tutorplatform.student.application.exception.invite.StudentInviteAlreadyAcceptedException;
import com.tutorplatform.student.application.exception.invite.StudentInviteNotFoundException;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentInviteStatus;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteEntity;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class StudentInviteService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final StudentInviteRepository studentInviteRepository;
    private final UserRepository userRepository;
    private final StudentInviteTokenService tokenService;
    private final Duration ttl;
    private final String publicFrontendBaseUrl;

    public StudentInviteService(
        TeacherRepository teacherRepository,
        StudentRepository studentRepository,
        StudentInviteRepository studentInviteRepository,
        UserRepository userRepository,
        StudentInviteTokenService tokenService,
        @Value("${app.student-invites.ttl}") Duration ttl,
        @Value("${app.student-invites.public-frontend-base-url}") URI publicFrontendBaseUrl
    ) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Student invite TTL must be positive");
        }
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.studentInviteRepository = studentInviteRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.ttl = ttl;
        this.publicFrontendBaseUrl = stripTrailingSlash(publicFrontendBaseUrl.toString());
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    @Transactional
    public StudentInviteCreatedResponse createInvite(
        AuthenticatedUser principal,
        UUID studentId,
        CreateStudentInviteRequest request
    ) {
        TeacherEntity teacher = currentTeacher(principal);
        StudentEntity student = studentRepository.findOwnedStudentForUpdate(teacher.id(), studentId)
            .orElseThrow(StudentNotFoundException::new);

        if (student.getUserId() != null) {
            throw new StudentAlreadyRegisteredException();
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant now = Instant.now();
        studentInviteRepository.revokeActiveInvites(studentId, now);

        StudentInviteTokenService.Token token = tokenService.createToken();
        StudentInviteEntity invite = studentInviteRepository.saveAndFlush(new StudentInviteEntity(
            UUID.randomUUID(),
            student,
            teacher,
            request.email(),
            token.hash(),
            now.plus(ttl)
        ));

        return new StudentInviteCreatedResponse(
            invite.getId(),
            studentId,
            invite.getEmail(),
            invite.getExpiresAt(),
            publicFrontendBaseUrl + "/invite/student/" + token.rawValue(),
            invite.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public StudentInviteListResponse listInvites(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = currentTeacher(principal).id();
        studentRepository.findOwnedStudent(teacherId, studentId)
            .orElseThrow(StudentNotFoundException::new);

        Instant now = Instant.now();
        return new StudentInviteListResponse(studentInviteRepository.findAllByStudent_IdOrderByCreatedAtDesc(studentId)
            .stream()
            .map(invite -> new StudentInviteSummaryResponse(
                invite.getId(),
                invite.getEmail(),
                status(invite, now),
                invite.getExpiresAt(),
                invite.getCreatedAt()
            ))
            .toList());
    }

    @Transactional
    public void revokeInvite(AuthenticatedUser principal, UUID studentId, UUID inviteId) {
        UUID teacherId = currentTeacher(principal).id();
        studentRepository.findOwnedStudent(teacherId, studentId)
            .orElseThrow(StudentNotFoundException::new);

        StudentInviteEntity invite = studentInviteRepository.findByIdAndStudent_Id(inviteId, studentId)
            .orElseThrow(StudentInviteNotFoundException::new);
        if (invite.getAcceptedAt() != null) {
            throw new StudentInviteAlreadyAcceptedException();
        }
        Instant now = Instant.now();
        if (invite.getRevokedAt() == null && invite.getExpiresAt().isAfter(now)) {
            invite.revoke(now);
        }
    }

    private TeacherEntity currentTeacher(AuthenticatedUser principal) {
        return teacherRepository.findByUserId(principal.id()).orElseThrow();
    }

    private StudentInviteStatus status(StudentInviteEntity invite, Instant now) {
        if (invite.getAcceptedAt() != null) {
            return StudentInviteStatus.ACCEPTED;
        }
        if (invite.getRevokedAt() != null) {
            return StudentInviteStatus.REVOKED;
        }
        if (!invite.getExpiresAt().isAfter(now)) {
            return StudentInviteStatus.EXPIRED;
        }
        return StudentInviteStatus.ACTIVE;
    }
}
