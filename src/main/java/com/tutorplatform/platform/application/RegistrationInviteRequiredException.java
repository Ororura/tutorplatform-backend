package com.tutorplatform.platform.application;

public class RegistrationInviteRequiredException extends RuntimeException {

    public RegistrationInviteRequiredException() {
        super("Registration is available by invitation only");
    }
}
