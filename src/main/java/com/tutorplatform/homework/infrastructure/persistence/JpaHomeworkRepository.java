package com.tutorplatform.homework.infrastructure.persistence;

import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaHomeworkRepository implements HomeworkRepository {

    private final HomeworkDatabaseRepository databaseRepository;
    private final HomeworkItemDatabaseRepository itemDatabaseRepository;

    JpaHomeworkRepository(
        HomeworkDatabaseRepository databaseRepository,
        HomeworkItemDatabaseRepository itemDatabaseRepository
    ) {
        this.databaseRepository = databaseRepository;
        this.itemDatabaseRepository = itemDatabaseRepository;
    }

    @Override
    public HomeworkEntity saveAndFlush(HomeworkEntity homework) {
        return databaseRepository.saveAndFlush(new HomeworkDatabaseModel(homework)).toEntity();
    }

    @Override
    public Optional<HomeworkEntity> findById(UUID homeworkId) {
        return findByIdWithItems(homeworkId);
    }

    @Override
    public Optional<HomeworkEntity> findByIdWithItems(UUID homeworkId) {
        return databaseRepository.findWithItemsById(homeworkId).map(HomeworkDatabaseModel::toEntity);
    }

    @Override
    public Optional<HomeworkEntity> findByHomeworkItemIdWithItems(UUID homeworkItemId) {
        return databaseRepository.findWithItemsByHomeworkItemId(homeworkItemId)
            .map(HomeworkDatabaseModel::toEntity);
    }

    @Override
    public List<HomeworkEntity> findAllByStudentProgramId(UUID studentProgramId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        return databaseRepository.findAllByStudentProgramIdOrderByAssignedAtDesc(
                studentProgramId, PageRequest.of(page, size)
            ).stream()
            .map(HomeworkDatabaseModel::getId)
            .map(databaseRepository::findWithItemsById)
            .flatMap(Optional::stream)
            .map(HomeworkDatabaseModel::toEntity)
            .toList();
    }

    @Override
    public boolean existsByIdAndStudentProgramId(UUID homeworkId, UUID studentProgramId) {
        return databaseRepository.existsByIdAndStudentProgramId(homeworkId, studentProgramId);
    }

    @Override
    public Optional<HomeworkItemEntity> findItemById(UUID homeworkId, UUID homeworkItemId) {
        return itemDatabaseRepository.findByHomeworkIdAndId(homeworkId, homeworkItemId)
            .map(item -> item.toEntity(homeworkId));
    }
}
