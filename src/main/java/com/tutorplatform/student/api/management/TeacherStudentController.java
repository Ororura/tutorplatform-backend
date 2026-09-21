package com.tutorplatform.student.api.management;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.application.management.StudentService;
import com.tutorplatform.student.domain.StudentAccountStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/students")
public class TeacherStudentController implements TeacherStudentApi {

    private final StudentService studentService;

    public TeacherStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    @GetMapping
    public StudentPageResponse listStudents(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StudentAccountStatus accountStatus,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return studentService.listStudents(principal, page, size, query, accountStatus, sort);
    }

    @PostMapping
    @Override
    public ResponseEntity<StudentSummaryResponse> createStudent(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateStudentRequest request) {
        StudentSummaryResponse response = studentService.createStudent(principal, request);

        return ResponseEntity.created(URI.create("/api/v1/teacher/students/" + response.id()))
                .body(response);
    }

    @GetMapping("/{studentId}")
    @Override
    public StudentDetailsResponse getStudent(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID studentId) {
        return studentService.getStudent(principal, studentId);
    }

    @PatchMapping("/{studentId}")
    @Override
    public UpdateStudentResponse updateStudent(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID studentId,
            @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.updateStudent(principal, studentId, request);
    }
}
