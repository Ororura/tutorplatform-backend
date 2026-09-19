package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserRole")
public enum UserRole {
    TEACHER,
    STUDENT,
    ADMIN
}
