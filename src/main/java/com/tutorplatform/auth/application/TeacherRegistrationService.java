package com.tutorplatform.auth.application;

import com.tutorplatform.auth.api.TeacherRegistrationRequest;
import com.tutorplatform.platform.application.RegistrationPolicyService;
import com.tutorplatform.user.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TeacherRegistrationService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationPolicyService registrationPolicyService;

    public TeacherRegistrationService(
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        PasswordEncoder passwordEncoder,
        RegistrationPolicyService registrationPolicyService
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationPolicyService = registrationPolicyService;
    }

    @Transactional
    public void registerTeacher(TeacherRegistrationRequest request) {
        registrationPolicyService.requireOpenRegistration();

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        UserEntity user = new UserEntity(
            UUID.randomUUID(),
            request.email(),
            passwordEncoder.encode(request.password()),
            UserStatus.ACTIVE
        );
        user.addRole(UserRole.TEACHER);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        teacherRepository.save(new TeacherEntity(UUID.randomUUID(), user, request.displayName()));
    }
}
