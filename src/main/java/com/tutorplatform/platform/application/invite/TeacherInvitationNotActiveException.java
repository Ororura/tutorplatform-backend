package com.tutorplatform.platform.application.invite;

public class TeacherInvitationNotActiveException extends RuntimeException {

    public TeacherInvitationNotActiveException() {
        super("Teacher registration invitation is not active");
    }
}
