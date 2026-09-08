package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.infrastructure.persistence.studentprogram.StudentProgramDatabaseModel;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "homeworks")
public class HomeworkDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_program_id", nullable = false, insertable = false, updatable = false)
    private StudentProgramDatabaseModel studentProgram;

    @Column(name = "student_program_id", nullable = false, updatable = false)
    private UUID studentProgramId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel assignedByTeacher;

    @Column(name = "assigned_by_teacher_id", nullable = false, updatable = false)
    private UUID assignedByTeacherId;

    @Column(nullable = false, length = 220)
    private String title;

    @Column
    private String description;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private HomeworkStatus status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "homework_id", nullable = false)
    @OrderBy("position ASC")
    private List<HomeworkItemDatabaseModel> items = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HomeworkDatabaseModel() {
    }

    HomeworkDatabaseModel(HomeworkEntity homework) {
        id = Objects.requireNonNull(homework.getId());
        studentProgramId = Objects.requireNonNull(homework.getStudentProgramId());
        assignedByTeacherId = Objects.requireNonNull(homework.getAssignedByTeacherId());
        assignedAt = Objects.requireNonNull(homework.getAssignedAt());
        updateFrom(homework);
        version = homework.getVersion();
    }

    void updateFrom(HomeworkEntity homework) {
        if (!Objects.requireNonNull(homework.getStudentProgramId()).equals(studentProgramId)
                || !Objects.requireNonNull(homework.getAssignedByTeacherId()).equals(assignedByTeacherId)
                || !Objects.requireNonNull(homework.getAssignedAt()).equals(assignedAt)) {
            throw new IllegalArgumentException("Homework assignment context cannot be changed");
        }
        title = Objects.requireNonNull(homework.getTitle());
        description = homework.getDescription();
        dueAt = homework.getDueAt();
        status = Objects.requireNonNull(homework.getStatus());
        completedAt = homework.getCompletedAt();
        items.clear();
        homework.getItems().stream()
                .map(HomeworkItemDatabaseModel::new)
                .forEach(items::add);
    }

    HomeworkEntity toEntity() {
        return toEntity(items.stream().map(item -> item.toEntity(id)).toList());
    }

    private HomeworkEntity toEntity(List<com.tutorplatform.homework.domain.HomeworkItemEntity> domainItems) {
        return new HomeworkEntity(
                id, studentProgramId, assignedByTeacherId, title, description, assignedAt, dueAt,
                status, completedAt, domainItems, version, createdAt, updatedAt
        );
    }

    UUID getId() { return id; }
    UUID getStudentProgramId() { return studentProgramId; }
    String getTitle() { return title; }
    HomeworkStatus getStatus() { return status; }
    Instant getAssignedAt() { return assignedAt; }
    Instant getDueAt() { return dueAt; }
    Instant getCompletedAt() { return completedAt; }
    Instant getCreatedAt() { return createdAt; }
}
