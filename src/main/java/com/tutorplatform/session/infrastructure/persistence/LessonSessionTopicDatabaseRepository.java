package com.tutorplatform.session.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

interface LessonSessionTopicDatabaseRepository
        extends JpaRepository<LessonSessionTopicDatabaseModel, LessonSessionTopicId> {
    List<LessonSessionTopicDatabaseModel> findAllByIdLessonSessionId(UUID lessonSessionId);

    List<LessonSessionTopicDatabaseModel> findAllByIdLessonSessionIdIn(Set<UUID> lessonSessionIds);

    @Modifying
    @Query("""
        delete from LessonSessionTopicDatabaseModel lessonSessionTopic
        where lessonSessionTopic.id.lessonSessionId = :lessonSessionId
        """)
    void deleteAllByLessonSessionId(@Param("lessonSessionId") UUID lessonSessionId);
}
