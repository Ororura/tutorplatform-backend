package com.tutorplatform.student.api;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.api.requrest.CreateStudentInviteRequest;
import com.tutorplatform.student.api.response.*;
import com.tutorplatform.student.application.StudentInviteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teacher/students/{studentId}/invites")
public class TeacherStudentInviteController implements TeacherStudentInviteApi {

    private final StudentInviteService studentInviteService;

    public TeacherStudentInviteController(StudentInviteService studentInviteService) {
        this.studentInviteService = studentInviteService;
    }

    @PostMapping
    @Override
    public ResponseEntity<StudentInviteCreatedResponse> createInvite(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID studentId, @Valid @RequestBody CreateStudentInviteRequest request) {
        StudentInviteCreatedResponse response = studentInviteService.createInvite(principal, studentId, request);

        return ResponseEntity.created(URI.create("/api/v1/teacher/students/" + studentId + "/invites/" + response.id())).body(response);
    }

    @GetMapping
    @Override
    public StudentInviteListResponse listInvites(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID studentId) {
        return studentInviteService.listInvites(principal, studentId);
    }

    @DeleteMapping("/{inviteId}")
    @Override
    public ResponseEntity<Void> revokeInvite(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID studentId, @PathVariable UUID inviteId) {
        studentInviteService.revokeInvite(principal, studentId, inviteId);

        return ResponseEntity.noContent().build();
    }
}
