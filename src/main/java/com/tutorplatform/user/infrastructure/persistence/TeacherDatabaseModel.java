package com.tutorplatform.user.infrastructure.persistence;

import com.tutorplatform.user.domain.TeacherEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teachers")
public class TeacherDatabaseModel {
    @Id private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, insertable = false, updatable = false)
    private UserDatabaseModel user;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TeacherDatabaseModel() {}

    TeacherDatabaseModel(TeacherEntity teacher) {
        id = teacher.id();
        updateFrom(teacher);
    }

    void updateFrom(TeacherEntity teacher) {
        userId = teacher.userId();
        displayName = teacher.displayName();
    }

    TeacherEntity toEntity() {
        return new TeacherEntity(id, userId, displayName, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
}
