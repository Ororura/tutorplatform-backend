package com.tutorplatform.homework.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkRepository {
    HomeworkEntity saveAndFlush(HomeworkEntity homework);
    Optional<HomeworkEntity> findById(UUID homeworkId);
    Optional<HomeworkEntity> findByIdWithItems(UUID homeworkId);
    List<HomeworkEntity> findAllByStudentProgramId(UUID studentProgramId, int page, int size);
    boolean existsByIdAndStudentProgramId(UUID homeworkId, UUID studentProgramId);
    Optional<HomeworkItemEntity> findItemById(UUID homeworkId, UUID homeworkItemId);
}
