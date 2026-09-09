package com.tutorplatform.subject.infrastructure.persistence;

import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class SubjectDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_teacher_id", insertable = false, updatable = false)
    private TeacherDatabaseModel ownerTeacher;

    @Column(name = "owner_teacher_id")
    private UUID ownerTeacherId;

    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SubjectStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubjectDatabaseModel() {
    }

    SubjectDatabaseModel(SubjectEntity subject) {
        id = Objects.requireNonNull(subject.id());
        updateFrom(subject);
    }

    void updateFrom(SubjectEntity subject) {
        ownerTeacherId = subject.ownerTeacherId();
        code = subject.code();
        name = Objects.requireNonNull(subject.name());
        description = subject.description();
        status = Objects.requireNonNull(subject.status());
    }

    SubjectEntity toEntity() {
        return new SubjectEntity(id, ownerTeacherId, code, name, description, status, createdAt, updatedAt);
    }
}
