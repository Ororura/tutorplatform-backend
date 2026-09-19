package com.tutorplatform.platform.application.invite;

public class TeacherInvitationEmailAlreadyRegisteredException extends RuntimeException {

    public TeacherInvitationEmailAlreadyRegisteredException() {
        super("Email is already registered");
    }
}
