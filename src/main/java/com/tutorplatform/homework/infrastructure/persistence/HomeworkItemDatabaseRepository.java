package com.tutorplatform.homework.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface HomeworkItemDatabaseRepository extends JpaRepository<HomeworkItemDatabaseModel, UUID> {

    @Query("""
        select item
        from HomeworkDatabaseModel homework
        join homework.items item
        where homework.id = :homeworkId
          and item.id = :homeworkItemId
        """)
    Optional<HomeworkItemDatabaseModel> findByHomeworkIdAndId(
            @Param("homeworkId") UUID homeworkId,
            @Param("homeworkItemId") UUID homeworkItemId
    );
}
