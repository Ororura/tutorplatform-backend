package com.tutorplatform.student.application;

import com.tutorplatform.auth.api.CurrentUserResponse;
import com.tutorplatform.auth.application.AuthenticationSessionService;
import com.tutorplatform.student.api.requrest.AcceptStudentInviteRequest;
import com.tutorplatform.student.api.response.PublicStudentInviteResponse;
import com.tutorplatform.student.application.exception.*;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteEntity;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PublicStudentInvitationService {

    private final StudentInviteTokenService tokenService;
    private final StudentInviteRepository studentInviteRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationSessionService authenticationSessionService;

    public PublicStudentInvitationService(
            StudentInviteTokenService tokenService,
            StudentInviteRepository studentInviteRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationSessionService authenticationSessionService
    ) {
        this.tokenService = tokenService;
        this.studentInviteRepository = studentInviteRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationSessionService = authenticationSessionService;
    }

    @Transactional(readOnly = true)
    public PublicStudentInviteResponse getInvitation(String rawToken) {
        StudentInviteEntity invite = studentInviteRepository.findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(StudentInviteNotFoundException::new);
        validateState(invite, Instant.now());

        return new PublicStudentInviteResponse(
                new PublicStudentInviteResponse.StudentName(
                        invite.getStudent().getFirstName(),
                        invite.getStudent().getLastName()
                ),
                new PublicStudentInviteResponse.TeacherName(invite.getCreatedByTeacher().getDisplayName()),
                invite.getEmail(),
                invite.getExpiresAt()
        );
    }

    @Transactional
    public CurrentUserResponse acceptInvitation(
            String rawToken,
            AcceptStudentInviteRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String tokenHash = tokenService.hash(rawToken);
        UUID studentId = studentInviteRepository.findStudentIdByTokenHash(tokenHash)
                .orElseThrow(StudentInviteNotFoundException::new);
        StudentEntity student = studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(StudentInviteNotFoundException::new);
        StudentInviteEntity invite = studentInviteRepository.findLockedByTokenHash(tokenHash)
                .orElseThrow(StudentInviteNotFoundException::new);
        Instant now = Instant.now();
        validateState(invite, now);

        if (student.getUserId() != null) {
            throw new StudentAlreadyRegisteredException();
        }
        if (userRepository.existsByEmail(invite.getEmail())) {
            throw new StudentInviteEmailConflictException();
        }

        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                invite.getEmail(),
                passwordEncoder.encode(request.password()),
                UserStatus.ACTIVE
        );
        user.addRole(UserRole.STUDENT);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new StudentInviteEmailConflictException();
        }

        student.linkUser(user.getId());
        invite.accept(now);
        studentRepository.saveAndFlush(student);
        studentInviteRepository.saveAndFlush(invite);

        return authenticationSessionService.authenticate(
                invite.getEmail(), request.password(), servletRequest, servletResponse
        );
    }

    private void validateState(StudentInviteEntity invite, Instant now) {
        if (invite.getAcceptedAt() != null) {
            throw new PublicStudentInviteAlreadyAcceptedException();
        }
        if (invite.getRevokedAt() != null) {
            throw new StudentInviteRevokedException();
        }
        if (!invite.getExpiresAt().isAfter(now)) {
            throw new StudentInviteExpiredException();
        }
    }
}
