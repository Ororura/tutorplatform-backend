package com.tutorplatform.submission.infrastructure.persistence;

import com.tutorplatform.homework.infrastructure.persistence.HomeworkItemDatabaseModel;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.StudentProgramDatabaseModel;
import com.tutorplatform.student.infrastructure.persistence.StudentDatabaseModel;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.infrastructure.persistence.task.TaskDatabaseModel;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "submissions")
public class SubmissionDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, insertable = false, updatable = false)
    private StudentDatabaseModel student;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_program_id", nullable = false, insertable = false, updatable = false)
    private StudentProgramDatabaseModel studentProgram;

    @Column(name = "student_program_id", nullable = false, updatable = false)
    private UUID studentProgramId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, insertable = false, updatable = false)
    private TaskDatabaseModel task;

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_item_id", insertable = false, updatable = false)
    private HomeworkItemDatabaseModel homeworkItem;

    @Column(name = "homework_item_id", updatable = false)
    private UUID homeworkItemId;

    @Column(name = "attempt_no", nullable = false, updatable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SubmissionStatus status;

    @Column(name = "text_answer")
    private String textAnswer;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SubmissionDatabaseModel() {
    }

    SubmissionDatabaseModel(SubmissionEntity submission) {
        id = Objects.requireNonNull(submission.getId());
        studentId = Objects.requireNonNull(submission.getStudentId());
        studentProgramId = Objects.requireNonNull(submission.getStudentProgramId());
        taskId = Objects.requireNonNull(submission.getTaskId());
        homeworkItemId = submission.getHomeworkItemId();
        attemptNo = submission.getAttemptNo();
        status = Objects.requireNonNull(submission.getStatus());
        textAnswer = submission.getTextAnswer();
        submittedAt = Objects.requireNonNull(submission.getSubmittedAt());
    }

    SubmissionEntity toEntity() {
        return new SubmissionEntity(
                id, studentId, studentProgramId, taskId, homeworkItemId, attemptNo, status,
                textAnswer, submittedAt, createdAt
        );
    }
}
