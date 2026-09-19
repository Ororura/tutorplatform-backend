package com.tutorplatform.platform.application.invite;

public class TeacherInvitationNotFoundException extends RuntimeException {

    public TeacherInvitationNotFoundException() {
        super("Teacher registration invitation not found");
    }
}
