package com.tutorplatform.task.infrastructure.persistence.topic;

import com.tutorplatform.program.infrastructure.persistence.TopicDatabaseModel;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.infrastructure.persistence.task.TaskDatabaseModel;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "topic_tasks")
public class TopicTaskDatabaseModel {

    @EmbeddedId private TopicTaskId id;

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

    protected TopicTaskDatabaseModel() {}

    TopicTaskDatabaseModel(TopicTaskEntity topicTask) {
        id = new TopicTaskId(topicTask.topicId(), topicTask.taskId());
        position = topicTask.position();
        required = topicTask.required();
    }

    TopicTaskEntity toEntity() {
        return new TopicTaskEntity(id.getTopicId(), id.getTaskId(), position, required, createdAt);
    }
}
