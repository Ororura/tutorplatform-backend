package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.user.infrastructure.persistence.UserDatabaseModel;
import jakarta.persistence.*;
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
    @JoinColumn(name = "user_id", unique = true, insertable = false, updatable = false)
    private UserDatabaseModel user;

    @Column(name = "user_id", unique = true)
    private UUID userId;

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
        updateFrom(student);
    }

    void updateFrom(StudentEntity student) {
        this.firstName = Objects.requireNonNull(student.getFirstName());
        this.lastName = student.getLastName();
        this.status = Objects.requireNonNull(student.getStatus());
        this.userId = student.getUserId();
    }

    StudentEntity toEntity() {
        return new StudentEntity(
            id,
            userId,
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
