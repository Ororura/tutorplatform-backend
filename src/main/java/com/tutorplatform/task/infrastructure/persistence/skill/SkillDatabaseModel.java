package com.tutorplatform.task.infrastructure.persistence.skill;

import com.tutorplatform.subject.infrastructure.persistence.SubjectDatabaseModel;
import com.tutorplatform.task.domain.skill.SkillEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "skills")
public class SkillDatabaseModel {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, insertable = false, updatable = false)
    private SubjectDatabaseModel subject;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private UUID subjectId;

    @Column(nullable = false, length = 100, updatable = false)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SkillDatabaseModel() {
    }

    SkillDatabaseModel(SkillEntity skill) {
        id = Objects.requireNonNull(skill.id());
        subjectId = Objects.requireNonNull(skill.subjectId());
        code = Objects.requireNonNull(skill.code());
        name = Objects.requireNonNull(skill.name());
        description = skill.description();
    }

    SkillEntity toEntity() {
        return new SkillEntity(id, subjectId, code, name, description, createdAt, updatedAt);
    }
}
