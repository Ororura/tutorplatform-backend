package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.user.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "students")
public class StudentDatabaseModel {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private StudentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentDatabaseModel() {
    }

    StudentDatabaseModel(StudentEntity student) {
        this.id = Objects.requireNonNull(student.getId());
        updateFrom(student, null);
    }

    void updateFrom(StudentEntity student, UserEntity user) {
        this.firstName = Objects.requireNonNull(student.getFirstName());
        this.lastName = student.getLastName();
        this.status = Objects.requireNonNull(student.getStatus());
        this.user = user;
    }

    StudentEntity toEntity() {
        return new StudentEntity(
                id,
                user == null ? null : user.getId(),
                firstName,
                lastName,
                status,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
