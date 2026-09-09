package com.tutorplatform.homework.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.application.exception.HomeworkNotFoundException;
import com.tutorplatform.homework.application.exception.HomeworkStudentProgramNotFoundException;
import com.tutorplatform.homework.application.exception.InvalidHomeworkException;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.StudentOwnershipQuery;
import com.tutorplatform.student.application.exception.StudentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentHomeworkService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "assignedAt", "dueAt", "createdAt", "title"
    );

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final HomeworkQuery homeworkQuery;

    public StudentHomeworkService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        HomeworkQuery homeworkQuery
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.homeworkQuery = homeworkQuery;
    }

    public StudentHomeworkPageResult listHomeworks(
        AuthenticatedUser principal,
        UUID studentProgramId,
        HomeworkStatus status,
        int page,
        int size,
        String sort
    ) {
        SortParameters sortParameters = validateListParameters(page, size, sort);
        UUID studentId = currentStudentId(principal);
        if (studentProgramId != null) {
            requireOwnedStudentProgram(studentId, studentProgramId);
        }
        StudentHomeworkPage result = homeworkQuery.findPageByStudent(
            studentId, studentProgramId, status, page, size,
            sortParameters.field(), sortParameters.ascending()
        );
        Instant now = Instant.now();
        return new StudentHomeworkPageResult(
            result.items().stream().map(item -> new StudentHomeworkSummaryResult(
                item.id(), item.studentProgramId(), item.title(), item.status(),
                item.assignedAt(), item.dueAt(), isOverdue(
                item.dueAt(), item.status(), item.completedAt(), now
            ), item.itemsCount(), item.createdAt()
            )).toList(),
            page,
            size,
            result.totalElements(),
            result.totalPages()
        );
    }

    public StudentHomeworkDetailsResult getHomework(
        AuthenticatedUser principal,
        UUID homeworkId
    ) {
        UUID studentId = currentStudentId(principal);
        StudentHomeworkDetails homework = homeworkQuery.findDetailsByStudent(studentId, homeworkId)
            .orElseThrow(HomeworkNotFoundException::new);
        return new StudentHomeworkDetailsResult(
            homework.id(), homework.studentProgramId(), homework.title(), homework.description(),
            homework.status(), homework.assignedAt(), homework.dueAt(), isOverdue(
            homework.dueAt(), homework.status(), homework.completedAt(), Instant.now()
        ), homework.completedAt(), homework.items()
        );
    }

    private UUID currentStudentId(AuthenticatedUser principal) {
        return studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
    }

    private void requireOwnedStudentProgram(UUID studentId, UUID studentProgramId) {
        ProgramQuery.StudentProgramContext studentProgram = programQuery
            .findStudentProgram(studentProgramId)
            .orElseThrow(HomeworkStudentProgramNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId)) {
            throw new HomeworkStudentProgramNotFoundException();
        }
    }

    private boolean isOverdue(
        Instant dueAt,
        HomeworkStatus status,
        Instant completedAt,
        Instant now
    ) {
        return dueAt != null
            && dueAt.isBefore(now)
            && status == HomeworkStatus.ASSIGNED
            && completedAt == null;
    }

    private SortParameters validateListParameters(int page, int size, String sort) {
        if (page < 0) {
            throw new InvalidHomeworkException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidHomeworkException("size", "must be between 1 and 100");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
            throw new InvalidHomeworkException(
                "sort", "must use assignedAt, dueAt, createdAt, or title"
            );
        }
        if (!parts[1].equals("asc") && !parts[1].equals("desc")) {
            throw new InvalidHomeworkException("sort", "direction must be asc or desc");
        }
        return new SortParameters(parts[0], parts[1].equals("asc"));
    }

    private record SortParameters(String field, boolean ascending) {
    }
}
