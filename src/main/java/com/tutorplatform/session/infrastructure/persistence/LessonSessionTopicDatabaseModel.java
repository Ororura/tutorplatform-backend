package com.tutorplatform.session.infrastructure.persistence;

import com.tutorplatform.program.infrastructure.persistence.TopicDatabaseModel;
import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "lesson_session_topics")
public class LessonSessionTopicDatabaseModel {

    @EmbeddedId private LessonSessionTopicId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_session_id", nullable = false, insertable = false, updatable = false)
    private LessonSessionDatabaseModel lessonSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, insertable = false, updatable = false)
    private TopicDatabaseModel topic;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LessonSessionTopicDatabaseModel() {}

    LessonSessionTopicDatabaseModel(LessonSessionTopicEntity lessonSessionTopic) {
        id =
                new LessonSessionTopicId(
                        lessonSessionTopic.lessonSessionId(), lessonSessionTopic.topicId());
        primary = lessonSessionTopic.primary();
    }

    LessonSessionTopicEntity toEntity() {
        return new LessonSessionTopicEntity(
                id.getLessonSessionId(), id.getTopicId(), primary, createdAt);
    }
}
