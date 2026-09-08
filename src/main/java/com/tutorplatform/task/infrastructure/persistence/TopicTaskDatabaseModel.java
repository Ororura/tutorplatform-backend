package com.tutorplatform.task.infrastructure.persistence;

import com.tutorplatform.program.infrastructure.persistence.TopicDatabaseModel;
import com.tutorplatform.task.domain.TopicTaskEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "topic_tasks")
public class TopicTaskDatabaseModel {

    @EmbeddedId
    private TopicTaskId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, insertable = false, updatable = false)
    private TopicDatabaseModel topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, insertable = false, updatable = false)
    private TaskDatabaseModel task;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean required;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TopicTaskDatabaseModel() {
    }

    TopicTaskDatabaseModel(TopicTaskEntity topicTask) {
        id = new TopicTaskId(topicTask.getTopicId(), topicTask.getTaskId());
        position = topicTask.getPosition();
        required = topicTask.isRequired();
    }

    TopicTaskEntity toEntity() {
        return new TopicTaskEntity(id.getTopicId(), id.getTaskId(), position, required, createdAt);
    }
}
