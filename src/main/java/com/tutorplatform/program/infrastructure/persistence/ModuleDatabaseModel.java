package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.infrastructure.persistence.learningprogram.LearningProgramDatabaseModel;
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
@Table(name = "modules")
public class ModuleDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_program_id", nullable = false, insertable = false, updatable = false)
    private LearningProgramDatabaseModel learningProgram;

    @Column(name = "learning_program_id", nullable = false)
    private UUID learningProgramId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column private String description;

    @Column(nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ModuleDatabaseModel() {
    }

    ModuleDatabaseModel(ModuleEntity module) {
        id = Objects.requireNonNull(module.getId());
        updateFrom(module);
    }

    void updateFrom(ModuleEntity module) {
        learningProgramId = Objects.requireNonNull(module.getLearningProgramId());
        title = Objects.requireNonNull(module.getTitle());
        description = module.getDescription();
        position = module.getPosition();
    }

    ModuleEntity toEntity() {
        return new ModuleEntity(id, learningProgramId, title, description, position, createdAt, updatedAt);
    }
}
