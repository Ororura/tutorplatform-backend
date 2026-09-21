package com.tutorplatform.auth.application;

import com.tutorplatform.auth.api.TeacherRegistrationRequest;
import com.tutorplatform.platform.application.RegistrationPolicyService;
import com.tutorplatform.platform.application.invite.TeacherRegistrationInviteService;
import com.tutorplatform.user.domain.*;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherRegistrationService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationPolicyService registrationPolicyService;
    private final TeacherRegistrationInviteService invitationService;

    public TeacherRegistrationService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            PasswordEncoder passwordEncoder,
            RegistrationPolicyService registrationPolicyService,
            TeacherRegistrationInviteService invitationService) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationPolicyService = registrationPolicyService;
        this.invitationService = invitationService;
    }

    @Transactional
    public void registerTeacher(TeacherRegistrationRequest request) {
        registrationPolicyService.requireOpenRegistration();
        createTeacher(request);
    }

    @Transactional
    public String registerInvitedTeacher(String rawToken, String displayName, String password) {
        var invitation = invitationService.lockActiveInvitation(rawToken);

        createTeacher(new TeacherRegistrationRequest(displayName, invitation.email(), password));

        invitationService.markInvitationAccepted(invitation.id());

        return invitation.email();
    }

    private void createTeacher(TeacherRegistrationRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        UserEntity user =
                new UserEntity(
                        UUID.randomUUID(),
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        teacherRepository.save(new TeacherEntity(UUID.randomUUID(), user, request.displayName()));
    }
}
