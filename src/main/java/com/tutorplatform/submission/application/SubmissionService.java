package com.tutorplatform.submission.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.application.ProgramQuery;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import com.tutorplatform.submission.application.exception.*;
import com.tutorplatform.submission.domain.*;
import com.tutorplatform.task.application.TaskQuery;
import com.tutorplatform.task.application.exception.TaskNotFoundException;
import com.tutorplatform.task.domain.task.TaskType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubmissionService {

    private static final Set<String> TEACHER_SORT_FIELDS =
            Set.of("submittedAt", "attemptNo", "status");

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final TaskQuery taskQuery;
    private final SubmissionHomeworkContextQuery homeworkContextQuery;
    private final ProgramQuery programQuery;
    private final SubmissionRepository submissionRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SubmissionService(
            StudentOwnershipQuery studentOwnershipQuery,
            TaskQuery taskQuery,
            SubmissionHomeworkContextQuery homeworkContextQuery,
            ProgramQuery programQuery,
            SubmissionRepository submissionRepository,
            CodeSubmissionRepository codeSubmissionRepository,
            ApplicationEventPublisher eventPublisher) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.taskQuery = taskQuery;
        this.homeworkContextQuery = homeworkContextQuery;
        this.programQuery = programQuery;
        this.submissionRepository = submissionRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SubmissionResult submitTextAnswer(
            AuthenticatedUser principal, UUID taskId, UUID homeworkItemId, String textAnswer) {
        if (homeworkItemId == null) {
            throw new InvalidSubmissionException("homeworkItemId", "must not be null");
        }
        if (textAnswer == null || textAnswer.isBlank()) {
            throw new InvalidSubmissionException("textAnswer", "must not be blank");
        }
        UUID studentId = currentStudentId(principal);
        requireTextTask(taskId);
        SubmissionHomeworkContextQuery.HomeworkSubmissionContext homework =
                requireHomeworkContext(studentId, taskId, homeworkItemId);
        if (homework.cancelled() || homework.completed()) {
            throw new HomeworkNotSubmittableException();
        }

        SubmissionAttemptContext attemptContext =
                new SubmissionAttemptContext(
                        studentId, homework.studentProgramId(), taskId, homeworkItemId);
        int attemptNo = submissionRepository.nextAttemptNo(attemptContext);
        SubmissionEntity saved =
                submissionRepository.saveAndFlush(
                        new SubmissionEntity(
                                UUID.randomUUID(),
                                studentId,
                                homework.studentProgramId(),
                                taskId,
                                homeworkItemId,
                                attemptNo,
                                SubmissionStatus.NEEDS_REVIEW,
                                textAnswer,
                                Instant.now()));
        return SubmissionResult.from(saved);
    }

    public SubmissionResult getStudentSubmission(AuthenticatedUser principal, UUID submissionId) {
        UUID studentId = currentStudentId(principal);
        return submissionRepository
                .findByIdAndStudentId(submissionId, studentId)
                .map(submission -> enrich(SubmissionResult.from(submission), true))
                .orElseThrow(SubmissionNotFoundException::new);
    }

    public SubmissionPageResult listStudentTaskSubmissions(
            AuthenticatedUser principal, UUID taskId, UUID homeworkItemId, int page, int size) {
        validatePage(page, size);
        UUID studentId = currentStudentId(principal);
        requireTask(taskId);
        SubmissionPage submissions;
        if (homeworkItemId == null) {
            submissions =
                    submissionRepository.findPageByStudentIdAndTaskId(
                            studentId, taskId, page, size);
        } else {
            SubmissionHomeworkContextQuery.HomeworkSubmissionContext homework =
                    requireHomeworkContext(studentId, taskId, homeworkItemId);
            submissions =
                    submissionRepository.findAttempts(
                            new SubmissionAttemptContext(
                                    studentId, homework.studentProgramId(), taskId, homeworkItemId),
                            page,
                            size);
        }
        var codeSubmissions =
                codeSubmissionRepository.findSummaries(
                        submissions.items().stream().map(SubmissionEntity::getId).toList());
        return new SubmissionPageResult(
                submissions.items().stream()
                        .map(
                                submission -> {
                                    SubmissionResult result = SubmissionResult.from(submission);
                                    CodeSubmissionRepository.Summary code =
                                            codeSubmissions.get(submission.getId());
                                    return code == null ? result : result.withCodeSubmission(code);
                                })
                        .toList(),
                page,
                size,
                submissions.totalElements(),
                submissions.totalPages());
    }

    public SubmissionPageResult listTeacherStudentSubmissions(
            AuthenticatedUser principal,
            UUID studentId,
            SubmissionStatus status,
            int page,
            int size,
            String sort) {
        SortParameters sortParameters = validateTeacherListParameters(page, size, sort);
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        SubmissionPage submissions =
                submissionRepository.findPageForTeacher(
                        studentId,
                        status,
                        page,
                        size,
                        sortParameters.field(),
                        sortParameters.ascending());
        return new SubmissionPageResult(
                submissions.items().stream().map(SubmissionResult::from).toList(),
                page,
                size,
                submissions.totalElements(),
                submissions.totalPages());
    }

    @Transactional
    public SubmissionResult reviewTextSubmission(
            AuthenticatedUser principal,
            UUID studentId,
            UUID submissionId,
            SubmissionStatus reviewStatus) {
        UUID teacherId = currentTeacherId(principal);
        requireOwnedStudent(teacherId, studentId);
        SubmissionEntity submission =
                submissionRepository
                        .findByIdAndStudentId(submissionId, studentId)
                        .orElseThrow(SubmissionNotFoundException::new);
        requireTeacherSubmissionContext(teacherId, studentId, submission);
        TaskQuery.TaskContext task =
                taskQuery
                        .findTask(submission.getTaskId())
                        .orElseThrow(SubmissionNotFoundException::new);
        if (task.type() != TaskType.TEXT) {
            throw new SubmissionNotReviewableException();
        }
        if (reviewStatus != SubmissionStatus.PASSED && reviewStatus != SubmissionStatus.FAILED) {
            throw new InvalidSubmissionReviewStatusException();
        }
        if (submission.getStatus() != SubmissionStatus.NEEDS_REVIEW) {
            throw new SubmissionNotReviewableException();
        }
        submission.review(reviewStatus);
        SubmissionEntity saved = submissionRepository.saveAndFlush(submission);
        eventPublisher.publishEvent(
                new SubmissionReviewedEvent(
                        saved.getId(),
                        saved.getStudentId(),
                        saved.getStudentProgramId(),
                        saved.getTaskId(),
                        saved.getHomeworkItemId(),
                        saved.getStatus()));
        return SubmissionResult.from(saved);
    }

    private UUID currentStudentId(AuthenticatedUser principal) {
        return studentOwnershipQuery
                .findStudentIdByUserId(principal.id())
                .orElseThrow(StudentNotFoundException::new);
    }

    private SubmissionResult enrich(SubmissionResult result, boolean includeSourceCode) {
        return codeSubmissionRepository
                .findBySubmissionId(result.id())
                .map(code -> result.withCodeSubmission(code, includeSourceCode))
                .orElse(result);
    }

    private UUID currentTeacherId(AuthenticatedUser principal) {
        return studentOwnershipQuery.findTeacherIdByUserId(principal.id()).orElseThrow();
    }

    private void requireOwnedStudent(UUID teacherId, UUID studentId) {
        if (!studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new StudentNotFoundException();
        }
    }

    private void requireTeacherSubmissionContext(
            UUID teacherId, UUID studentId, SubmissionEntity submission) {
        if (submission.getHomeworkItemId() == null) {
            return;
        }
        SubmissionHomeworkContextQuery.HomeworkSubmissionContext homework =
                homeworkContextQuery
                        .findSubmissionContext(submission.getHomeworkItemId())
                        .orElseThrow(SubmissionNotFoundException::new);
        ProgramQuery.StudentProgramContext program =
                programQuery
                        .findStudentProgram(homework.studentProgramId())
                        .orElseThrow(SubmissionNotFoundException::new);
        if (!homework.studentProgramId().equals(submission.getStudentProgramId())
                || !homework.taskId().equals(submission.getTaskId())
                || !homework.assignedByTeacherId().equals(teacherId)
                || !program.belongsToStudent(studentId)
                || !program.isAssignedBy(teacherId)) {
            throw new SubmissionNotFoundException();
        }
    }

    private TaskQuery.TaskContext requireTask(UUID taskId) {
        return taskQuery.findTask(taskId).orElseThrow(TaskNotFoundException::new);
    }

    private void requireTextTask(UUID taskId) {
        if (requireTask(taskId).type() != TaskType.TEXT) {
            throw new TextSubmissionRequiredException();
        }
    }

    private SubmissionHomeworkContextQuery.HomeworkSubmissionContext requireHomeworkContext(
            UUID studentId, UUID taskId, UUID homeworkItemId) {
        SubmissionHomeworkContextQuery.HomeworkSubmissionContext homework =
                homeworkContextQuery
                        .findSubmissionContext(homeworkItemId)
                        .orElseThrow(HomeworkItemNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram =
                programQuery
                        .findStudentProgram(homework.studentProgramId())
                        .orElseThrow(HomeworkItemNotFoundException::new);
        if (!studentProgram.belongsToStudent(studentId)) {
            throw new HomeworkItemNotFoundException();
        }
        if (!homework.taskId().equals(taskId)) {
            throw new SubmissionContextInvalidException();
        }
        return homework;
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidSubmissionException("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new InvalidSubmissionException("size", "must be between 1 and 100");
        }
    }

    private SortParameters validateTeacherListParameters(int page, int size, String sort) {
        validatePage(page, size);
        String value = sort == null || sort.isBlank() ? "submittedAt,desc" : sort;
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !TEACHER_SORT_FIELDS.contains(parts[0])) {
            throw new InvalidSubmissionException("sort", "must use an allowed field and direction");
        }
        if (!parts[1].equalsIgnoreCase("asc") && !parts[1].equalsIgnoreCase("desc")) {
            throw new InvalidSubmissionException("sort", "direction must be asc or desc");
        }
        return new SortParameters(parts[0], parts[1].equalsIgnoreCase("asc"));
    }

    private record SortParameters(String field, boolean ascending) {}
}
