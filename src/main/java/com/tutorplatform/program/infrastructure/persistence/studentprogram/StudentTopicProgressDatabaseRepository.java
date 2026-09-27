package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface StudentTopicProgressDatabaseRepository
        extends JpaRepository<StudentTopicProgressDatabaseModel, StudentTopicProgressId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
        select progress
        from StudentTopicProgressDatabaseModel progress
        where progress.id.studentProgramId = :studentProgramId
          and progress.id.topicId in :topicIds
        """)
    List<StudentTopicProgressDatabaseModel> findAllForUpdate(
            UUID studentProgramId, List<UUID> topicIds);
}
