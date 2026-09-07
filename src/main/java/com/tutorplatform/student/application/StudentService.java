package com.tutorplatform.student.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.api.requrest.CreateStudentRequest;
import com.tutorplatform.student.api.response.StudentAccountResponse;
import com.tutorplatform.student.api.response.StudentDetailsResponse;
import com.tutorplatform.student.api.response.StudentPageResponse;
import com.tutorplatform.student.api.response.StudentRelationResponse;
import com.tutorplatform.student.api.response.StudentSummaryResponse;
import com.tutorplatform.student.api.requrest.UpdateStudentRequest;
import com.tutorplatform.student.api.response.UpdateStudentResponse;
import com.tutorplatform.student.application.exception.InvalidStudentListParameterException;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import com.tutorplatform.student.domain.StudentAccountStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.StudentQueryRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.user.infrastructure.persistence.TeacherEntity;
import com.tutorplatform.user.infrastructure.persistence.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "firstName", "lastName");

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TeacherStudentLinkRepository teacherStudentLinkRepository;
    private final StudentQueryRepository studentQueryRepository;

    public StudentService(
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            TeacherStudentLinkRepository teacherStudentLinkRepository,
            StudentQueryRepository studentQueryRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.teacherStudentLinkRepository = teacherStudentLinkRepository;
        this.studentQueryRepository = studentQueryRepository;
    }

    @Transactional
    public StudentSummaryResponse createStudent(AuthenticatedUser principal, CreateStudentRequest request) {
        TeacherEntity teacher = currentTeacher(principal);
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
                UUID.randomUUID(),
                request.firstName(),
                request.lastName(),
                StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));

        return new StudentSummaryResponse(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getStatus(),
                StudentAccountStatus.UNREGISTERED,
                student.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public StudentPageResponse listStudents(
            AuthenticatedUser principal,
            int page,
            int size,
            String query,
            StudentAccountStatus accountStatus,
            String sort
    ) {
        UUID teacherId = currentTeacher(principal).getId();
        ListParameters parameters = validateListParameters(page, size, query, sort);
        StudentQueryRepository.StudentPage result = studentQueryRepository.findStudents(
                teacherId,
                page,
                size,
                parameters.searchPattern(),
                accountStatus,
                parameters.sortField(),
                parameters.ascending()
        );

        var items = result.items().stream()
                .map(row -> new StudentSummaryResponse(
                        row.id(),
                        row.firstName(),
                        row.lastName(),
                        row.status(),
                        row.accountStatus(),
                        row.createdAt()
                ))
                .toList();
        int totalPages = Math.toIntExact((result.totalElements() + size - 1) / size);
        return new StudentPageResponse(items, page, size, result.totalElements(), totalPages);
    }

    @Transactional(readOnly = true)
    public StudentDetailsResponse getStudent(AuthenticatedUser principal, UUID studentId) {
        UUID teacherId = currentTeacher(principal).getId();
        return toDetails(studentQueryRepository.findDetails(teacherId, studentId)
                .orElseThrow(StudentNotFoundException::new));
    }

    @Transactional
    public UpdateStudentResponse updateStudent(
            AuthenticatedUser principal,
            UUID studentId,
            UpdateStudentRequest request
    ) {
        UUID teacherId = currentTeacher(principal).getId();
        StudentEntity student = studentRepository.findOwnedStudent(teacherId, studentId)
                .orElseThrow(StudentNotFoundException::new);
        student.updateNames(
                request.firstName() == null ? student.getFirstName() : request.firstName(),
                request.lastName() == null ? student.getLastName() : request.lastName()
        );
        studentRepository.saveAndFlush(student);

        StudentQueryRepository.StudentDetailsRow updated = studentQueryRepository.findDetails(teacherId, studentId)
                .orElseThrow(StudentNotFoundException::new);
        return new UpdateStudentResponse(
                updated.id(),
                updated.firstName(),
                updated.lastName(),
                updated.status(),
                updated.accountStatus(),
                updated.updatedAt()
        );
    }

    private TeacherEntity currentTeacher(AuthenticatedUser principal) {
        return teacherRepository.findByUserId(principal.id()).orElseThrow();
    }

    private ListParameters validateListParameters(int page, int size, String query, String sort) {
        if (page < 0) {
            throw new InvalidStudentListParameterException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidStudentListParameterException("size", "must be between 1 and 100");
        }

        String normalizedQuery = query == null ? null : query.strip();
        if (normalizedQuery != null && normalizedQuery.length() > 100) {
            throw new InvalidStudentListParameterException("query", "size must be between 0 and 100");
        }
        String searchPattern = normalizedQuery == null || normalizedQuery.isEmpty()
                ? null
                : "%" + escapeLike(normalizedQuery.toLowerCase(Locale.ROOT)) + "%";

        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !ALLOWED_SORT_FIELDS.contains(sortParts[0])) {
            throw new InvalidStudentListParameterException(
                    "sort",
                    "must use createdAt, firstName, or lastName"
            );
        }
        if (!sortParts[1].equals("asc") && !sortParts[1].equals("desc")) {
            throw new InvalidStudentListParameterException("sort", "direction must be asc or desc");
        }

        return new ListParameters(searchPattern, sortParts[0], sortParts[1].equals("asc"));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private StudentDetailsResponse toDetails(StudentQueryRepository.StudentDetailsRow row) {
        return new StudentDetailsResponse(
                row.id(),
                row.firstName(),
                row.lastName(),
                row.status(),
                new StudentAccountResponse(row.accountStatus(), row.accountEmail()),
                new StudentRelationResponse(
                        StudentRelationResponse.RelationType.valueOf(row.relationType().name()),
                        row.relationStartedAt()
                ),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private record ListParameters(String searchPattern, String sortField, boolean ascending) {
    }
}
