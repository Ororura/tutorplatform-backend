package com.tutorplatform.program.infrastructure.persistence;

import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "topics")
public class TopicDatabaseModel {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, insertable = false, updatable = false)
    private ModuleDatabaseModel module;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column private String description;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TopicStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TopicDatabaseModel() {
    }

    TopicDatabaseModel(TopicEntity topic) {
        id = Objects.requireNonNull(topic.id());
        updateFrom(topic);
        version = topic.version();
    }

    void updateFrom(TopicEntity topic) {
        moduleId = Objects.requireNonNull(topic.moduleId());
        title = Objects.requireNonNull(topic.title());
        description = topic.description();
        position = topic.position();
        status = Objects.requireNonNull(topic.status());
    }

    TopicEntity toEntity() {
        return new TopicEntity(id, moduleId, title, description, position, status, version, createdAt, updatedAt);
    }
}
